/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.refactoring.extractFunction

import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.types.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle
import org.jetbrains.jet.lang.psi.psiUtil.isInsideOf
import java.util.*
import org.jetbrains.jet.plugin.refactoring.createTempCopy
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.jet.lang.cfg.pseudocode.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.cfg.Label
import org.jetbrains.jet.lang.psi.JetPsiFactory.FunctionBuilder
import org.jetbrains.jet.plugin.refactoring.JetNameValidatorImpl
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil
import org.jetbrains.jet.plugin.imports.canBeReferencedViaImport
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.lang.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.jet.utils.DFS
import org.jetbrains.jet.utils.DFS.*
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import org.jetbrains.jet.plugin.caches.resolve.getLazyResolveSession
import org.jetbrains.jet.lang.psi.psiUtil.prependElement
import org.jetbrains.jet.lang.psi.psiUtil.appendElement
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.psi.psiUtil.replaced
import org.jetbrains.jet.plugin.refactoring.extractFunction.AnalysisResult.Status
import org.jetbrains.jet.plugin.refactoring.extractFunction.AnalysisResult.ErrorMessage
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.*
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.eval.*
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.jumps.*
import kotlin.properties.Delegates
import org.jetbrains.jet.lang.cfg.pseudocodeTraverser.traverse
import org.jetbrains.jet.lang.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getTargetFunctionDescriptor
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.jet.lang.resolve.OverridingUtil

private val DEFAULT_FUNCTION_NAME = "myFun"
private val DEFAULT_RETURN_TYPE = KotlinBuiltIns.getInstance().getUnitType()
private val DEFAULT_PARAMETER_TYPE = KotlinBuiltIns.getInstance().getAnyType()

private fun JetType.isDefault(): Boolean = KotlinBuiltIns.getInstance().isUnit(this)

private fun List<Instruction>.getModifiedVarDescriptors(bindingContext: BindingContext): Set<VariableDescriptor> {
    return this
            .map {if (it is WriteValueInstruction) PseudocodeUtil.extractVariableDescriptorIfAny(it, true, bindingContext) else null}
            .filterNotNullTo(HashSet<VariableDescriptor>())
}

private fun List<Instruction>.getExitPoints(): List<Instruction> =
        filter { localInstruction -> localInstruction.nextInstructions.any { it !in this } }

private fun List<Instruction>.getResultType(
        pseudocode: Pseudocode,
        bindingContext: BindingContext,
        options: ExtractionOptions): JetType {
    fun instructionToType(instruction: Instruction): JetType? {
        val expression = when (instruction) {
            is ReturnValueInstruction ->
                (instruction.element as JetReturnExpression).getReturnedExpression()
            is InstructionWithValue ->
                instruction.outputValue?.element as? JetExpression
            else -> null
        }

        if (expression == null) return null
        if (options.inferUnitTypeForUnusedValues && expression.isStatement(pseudocode)) return null

        return bindingContext[BindingContext.EXPRESSION_TYPE, expression]
    }

    val resultTypes = map(::instructionToType).filterNotNull()
    return if (resultTypes.isNotEmpty()) CommonSupertypes.commonSupertype(resultTypes) else DEFAULT_RETURN_TYPE
}

private fun List<AbstractJumpInstruction>.checkEquivalence(checkPsi: Boolean): Boolean {
    if (mapTo(HashSet<Label?>()) { it.targetLabel }.size > 1) return false
    return !checkPsi || mapTo(HashSet<String?>()) { it.element.getText() }.size <= 1
}

private fun JetType.isMeaningful(): Boolean {
    return KotlinBuiltIns.getInstance().let { builtins -> !builtins.isUnit(this) && !builtins.isNothing(this) }
}

private fun List<Instruction>.analyzeControlFlow(
        pseudocode: Pseudocode,
        bindingContext: BindingContext,
        options: ExtractionOptions,
        parameters: Set<Parameter>
): Pair<ControlFlow, ErrorMessage?> {
    fun isCurrentFunctionReturn(expression: JetReturnExpression): Boolean {
        val functionDescriptor = expression.getTargetFunctionDescriptor(bindingContext)
        val currentDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, pseudocode.getCorrespondingElement()]
        return currentDescriptor == functionDescriptor
    }

    val exitPoints = getExitPoints()

    val valuedReturnExits = ArrayList<ReturnValueInstruction>()
    val defaultExits = ArrayList<Instruction>()
    val jumpExits = ArrayList<AbstractJumpInstruction>()
    exitPoints.forEach {
        val e = (it as? UnconditionalJumpInstruction)?.element
        val insn =
                when {
                    it !is ReturnValueInstruction && it !is ReturnNoValueInstruction && it.owner != pseudocode ->
                        null
                    e != null && e !is JetBreakExpression && e !is JetContinueExpression ->
                        it.previousInstructions.firstOrNull()
                    else ->
                        it
                }

        when (insn) {
            is ReturnValueInstruction ->
                    if (isCurrentFunctionReturn(insn.element as JetReturnExpression)) {
                        valuedReturnExits.add(insn)
                    }

            is AbstractJumpInstruction -> {
                val element = insn.element
                if ((element is JetReturnExpression && isCurrentFunctionReturn(element))
                        || element is JetBreakExpression
                        || element is JetContinueExpression) {
                    jumpExits.add(insn)
                }
                else if (element !is JetThrowExpression) {
                    defaultExits.add(insn)
                }
            }

            else -> if (insn != null && insn !is LocalFunctionDeclarationInstruction) {
                defaultExits.add(insn)
            }
        }
    }

    val typeOfDefaultFlow = defaultExits.getResultType(pseudocode, bindingContext, options)
    val returnValueType = valuedReturnExits.getResultType(pseudocode, bindingContext, options)
    val defaultControlFlow = DefaultControlFlow(if (returnValueType.isMeaningful()) returnValueType else typeOfDefaultFlow)

    val outParameters = parameters.filterTo(HashSet<Parameter>()) { it.mirrorVarName != null }
    when {
        outParameters.size > 1 -> {
            val outValuesStr = outParameters.map { it.argumentText }.sort()

            return Pair(
                    defaultControlFlow,
                    ErrorMessage.MULTIPLE_OUTPUT.addAdditionalInfo(outValuesStr)
            )
        }

        outParameters.size == 1 -> {
            if (returnValueType.isMeaningful() || typeOfDefaultFlow.isMeaningful() || jumpExits.isNotEmpty()) {
                return Pair(defaultControlFlow, ErrorMessage.OUTPUT_AND_EXIT_POINT)
            }

            return Pair(ParameterUpdate(outParameters.first()), null)
        }
    }

    val multipleExitsError = Pair(defaultControlFlow, ErrorMessage.MULTIPLE_EXIT_POINTS)

    if (typeOfDefaultFlow.isMeaningful()) {
        if (valuedReturnExits.isNotEmpty() || jumpExits.isNotEmpty()) return multipleExitsError

        return Pair(ExpressionEvaluation(typeOfDefaultFlow), null)
    }

    if (valuedReturnExits.isNotEmpty()) {
        if (jumpExits.isNotEmpty()) return multipleExitsError

        if (defaultExits.isNotEmpty()) {
            if (valuedReturnExits.size != 1) return multipleExitsError

            val element = valuedReturnExits.first!!.element
            return Pair(ConditionalJump(listOf(element), element), null)
        }

        if (!valuedReturnExits.checkEquivalence(false)) return multipleExitsError
        return Pair(ExpressionEvaluationWithCallSiteReturn(valuedReturnExits.getResultType(pseudocode, bindingContext, options)), null)
    }

    if (jumpExits.isNotEmpty()) {
        if (!jumpExits.checkEquivalence(true)) return multipleExitsError

        val elements = jumpExits.map { it.element }
        if (defaultExits.isNotEmpty()) return Pair(ConditionalJump(elements, elements.first!!), null)
        return Pair(UnconditionalJump(elements, elements.first!!), null)
    }

    return Pair(defaultControlFlow, null)
}

fun ExtractionData.createTemporaryFunction(functionText: String): JetNamedFunction {
    val textRange = targetSibling.getTextRange()!!

    val insertText: String
    val insertPosition: Int
    val lookupPosition: Int
    if (insertBefore) {
        insertPosition = textRange.getStartOffset()
        lookupPosition = insertPosition
        insertText = functionText
    }
    else {
        insertPosition = textRange.getEndOffset()
        lookupPosition = insertPosition + 1
        insertText = "\n$functionText"
    }

    val tmpFile = originalFile.createTempCopy { text ->
        StringBuilder(text).insert(insertPosition, insertText).toString()
    }
    return tmpFile.findElementAt(lookupPosition)?.getParentByType(javaClass<JetNamedFunction>())!!
}

private fun ExtractionData.createTemporaryCodeBlock(): JetBlockExpression =
        createTemporaryFunction("fun() {\n${getCodeFragmentText()}\n}\n").getBodyExpression() as JetBlockExpression

private fun JetType.collectReferencedTypes(processTypeArguments: Boolean): List<JetType> {
    if (!processTypeArguments) return Collections.singletonList(this)
    return DFS.dfsFromNode(
            this,
            object: Neighbors<JetType> {
                override fun getNeighbors(current: JetType): Iterable<JetType> = current.getArguments().map { it.getType() }
            },
            VisitedWithSet(),
            object: CollectingNodeHandler<JetType, JetType, ArrayList<JetType>>(ArrayList()) {
                override fun afterChildren(current: JetType) {
                    result.add(current)
                }
            }
    )!!
}

fun JetTypeParameter.collectRelevantConstraints(): List<JetTypeConstraint> {
    val typeConstraints = getParentByType(javaClass<JetTypeParameterListOwner>())?.getTypeConstraints()
    if (typeConstraints == null) return Collections.emptyList()
    return typeConstraints.filter { it.getSubjectTypeParameterName()?.getReference()?.resolve() == this}
}

fun TypeParameter.collectReferencedTypes(bindingContext: BindingContext): List<JetType> {
    val typeRefs = ArrayList<JetTypeReference>()
    originalDeclaration.getExtendsBound()?.let { typeRefs.add(it) }
    originalConstraints
            .map { it.getBoundTypeReference() }
            .filterNotNullTo(typeRefs)

    return typeRefs
            .map { bindingContext[BindingContext.TYPE, it] }
            .filterNotNull()
}

private fun JetType.processTypeIfExtractable(
        bindingContext: BindingContext,
        typeParameters: MutableSet<TypeParameter>,
        nonDenotableTypes: MutableSet<JetType>,
        processTypeArguments: Boolean = true
): Boolean {
    return collectReferencedTypes(processTypeArguments).fold(true) { (extractable, typeToCheck) ->
        val parameterTypeDescriptor = typeToCheck.getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor
        val typeParameter = parameterTypeDescriptor?.let {
            BindingContextUtils.descriptorToDeclaration(bindingContext, it)
        } as? JetTypeParameter

        when {
            typeParameter != null -> {
                typeParameters.add(TypeParameter(typeParameter, typeParameter.collectRelevantConstraints()))
                extractable
            }

            typeToCheck.canBeReferencedViaImport() ->
                extractable

            else -> {
                nonDenotableTypes.add(typeToCheck)
                false
            }
        }
    }
}

private class MutableParameter(
        override val argumentText: String,
        override val name: String,
        override val mirrorVarName: String?,
        override val receiverCandidate: Boolean
): Parameter {
    // All modifications happen in the same thread
    private var writable: Boolean = true
    private val defaultTypes = HashSet<JetType>()
    private val typePredicates = HashSet<TypePredicate>()

    fun addDefaultType(jetType: JetType) {
        assert(writable, "Can't add type to non-writable parameter $name")
        defaultTypes.add(jetType)
    }

    fun addTypePredicate(predicate: TypePredicate) {
        assert(writable, "Can't add type predicate to non-writable parameter $name")
        typePredicates.add(predicate)
    }

    override val parameterTypeCandidates: List<JetType> by Delegates.lazy {
        writable = false
        listOf(parameterType) + TypeUtils.getAllSupertypes(parameterType).filter(and(typePredicates))
    }

    override val parameterType: JetType by Delegates.lazy {
        writable = false
        CommonSupertypes.commonSupertype(defaultTypes)
    }

    override fun copy(name: String, parameterType: JetType): Parameter = DelegatingParameter(this, name, parameterType)
}

private class DelegatingParameter(
        val original: Parameter,
        override val name: String,
        override val parameterType: JetType
): Parameter by original {
    override fun copy(name: String, parameterType: JetType): Parameter = DelegatingParameter(original, name, parameterType)
}

private fun ExtractionData.inferParametersInfo(
        commonParent: PsiElement,
        pseudocode: Pseudocode,
        localInstructions: List<Instruction>,
        bindingContext: BindingContext,
        replacementMap: MutableMap<Int, Replacement>,
        parameters: MutableSet<Parameter>,
        typeParameters: MutableSet<TypeParameter>,
        nonDenotableTypes: MutableSet<JetType>
): ErrorMessage? {
    val varNameValidator = JetNameValidatorImpl(
            commonParent.getParentByType(javaClass<JetExpression>()),
            originalElements.first,
            JetNameValidatorImpl.Target.PROPERTIES
    )
    val modifiedVarDescriptors = localInstructions.getModifiedVarDescriptors(bindingContext)

    val extractedDescriptorToParameter = HashMap<DeclarationDescriptor, MutableParameter>()

    val valueUsageMap = pseudocode.collectValueUsages()

    for (refInfo in getBrokenReferencesInfo(createTemporaryCodeBlock())) {
        val (originalRef, originalDeclaration, originalDescriptor, resolvedCall) = refInfo.resolveResult
        val ref = refInfo.refExpr

        val selector = (ref.getParent() as? JetCallExpression) ?: ref
        val superExpr = (selector.getParent() as? JetQualifiedExpression)?.getReceiverExpression() as? JetSuperExpression
        if (superExpr != null) {
            return ErrorMessage.SUPER_CALL
        }

        val receiverArgument = resolvedCall?.getReceiverArgument()
        val receiver = when(receiverArgument) {
            ReceiverValue.NO_RECEIVER -> resolvedCall?.getThisObject()
            else -> receiverArgument
        } ?: ReceiverValue.NO_RECEIVER

        val thisDescriptor = (receiver as? ThisReceiver)?.getDeclarationDescriptor()
        val hasThisReceiver = thisDescriptor != null
        val thisExpr = ref.getParent() as? JetThisExpression

        val referencedClassDescriptor: ClassDescriptor? = (thisDescriptor ?: originalDescriptor).let {
            when (it) {
                is ClassDescriptor ->
                    when(it.getKind()) {
                        ClassKind.OBJECT, ClassKind.ENUM_CLASS -> it as ClassDescriptor
                        ClassKind.CLASS_OBJECT, ClassKind.ENUM_ENTRY -> it.getContainingDeclaration() as? ClassDescriptor
                        else -> if (ref.getParentByType(javaClass<JetTypeReference>()) != null) it as ClassDescriptor else null
                    }

                is ConstructorDescriptor -> it.getContainingDeclaration()

                else -> null
            }
        }

        if (referencedClassDescriptor != null) {
            if (!referencedClassDescriptor.getDefaultType().processTypeIfExtractable(
                    bindingContext, typeParameters, nonDenotableTypes, false
            )) continue

            val replacingDescriptor = (originalDescriptor as? ConstructorDescriptor)?.getContainingDeclaration() ?: originalDescriptor
            replacementMap[refInfo.offsetInBody] = FqNameReplacement(DescriptorUtils.getFqNameSafe(replacingDescriptor))
        }
        else {
            val extractThis = hasThisReceiver || thisExpr != null
            val extractLocalVar =
                    originalDeclaration is JetMultiDeclarationEntry ||
                            (originalDeclaration is JetProperty && originalDeclaration.isLocal()) ||
                            originalDeclaration is JetParameter

            val descriptorToExtract = (if (extractThis) thisDescriptor else null) ?: originalDescriptor

            val extractParameter = extractThis || extractLocalVar
            if (extractParameter) {
                val parameterType = when {
                    receiver.exists() -> receiver.getType()
                    else -> bindingContext[BindingContext.AUTOCAST, originalRef]
                            ?: bindingContext[BindingContext.EXPRESSION_TYPE, originalRef]
                            ?: DEFAULT_PARAMETER_TYPE
                }

                if (!parameterType.processTypeIfExtractable(bindingContext, typeParameters, nonDenotableTypes)) continue

                val parameterTypePredicate =
                        pseudocode.getElementValue(originalRef)?.let { getExpectedTypePredicate(it, valueUsageMap, bindingContext) } ?: AllTypes

                val parameter = extractedDescriptorToParameter.getOrPut(descriptorToExtract) {
                    val parameterName =
                            if (extractThis) {
                                JetNameSuggester.suggestNames(parameterType, varNameValidator, null).first()
                            }
                            else originalDeclaration.getName()!!

                    val mirrorVarName =
                            if (descriptorToExtract in modifiedVarDescriptors) varNameValidator.validateName(parameterName)!! else null

                    val argumentText =
                            if (hasThisReceiver && extractThis)
                                "this@${parameterType.getConstructor().getDeclarationDescriptor()!!.getName().asString()}"
                            else
                                (thisExpr ?: ref).getText() ?: throw AssertionError("'this' reference shouldn't be empty: code fragment = ${getCodeFragmentText()}")

                    MutableParameter(argumentText, parameterName, mirrorVarName, extractThis)
                }

                parameter.addDefaultType(parameterType)
                parameter.addTypePredicate(parameterTypePredicate)

                replacementMap[refInfo.offsetInBody] =
                        if (hasThisReceiver && extractThis) AddPrefixReplacement(parameter) else RenameReplacement(parameter)
            }
        }
    }

    for (typeToCheck in typeParameters.flatMapTo(HashSet<JetType>()) { it.collectReferencedTypes(bindingContext) }) {
        typeToCheck.processTypeIfExtractable(bindingContext, typeParameters, nonDenotableTypes)
    }

    parameters.addAll(extractedDescriptorToParameter.values())

    return null
}

private fun ExtractionData.checkLocalDeclarationsWithNonLocalUsages(
        pseudocode: Pseudocode,
        localInstructions: List<Instruction>,
        bindingContext: BindingContext
): ErrorMessage? {
    // todo: non-locally used declaration can be turned into the output value

    val declarations = ArrayList<JetNamedDeclaration>()
    pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
        if (instruction !in localInstructions) {
            PseudocodeUtil.extractVariableDescriptorIfAny(instruction, true, bindingContext)?.let { descriptor ->
                val declaration = DescriptorToDeclarationUtil.getDeclaration(project, descriptor, bindingContext)
                if (declaration is JetNamedDeclaration && declaration.isInsideOf(originalElements)) {
                    declarations.add(declaration)
                }
            }
        }
    }

    if (declarations.isNotEmpty()) {
        val localVarStr = declarations.map { it.getName()!! }.sort()
        return ErrorMessage.VARIABLES_ARE_USED_OUTSIDE.addAdditionalInfo(localVarStr)
    }

    return null
}

private fun ExtractionData.checkDeclarationsMovingOutOfScope(controlFlow: ControlFlow): ErrorMessage? {
    val declarationsOutOfScope = HashSet<JetNamedDeclaration>()
    if (controlFlow is JumpBasedControlFlow) {
        controlFlow.elementToInsertAfterCall.accept(
                object: JetTreeVisitorVoid() {
                    override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                        val target = expression.getReference()?.resolve()
                        if (target is JetNamedDeclaration && target.isInsideOf(originalElements)) {
                            declarationsOutOfScope.add(target)
                        }
                    }
                }
        )
    }

    if (declarationsOutOfScope.isNotEmpty()) {
        val declStr = declarationsOutOfScope.map { it.getName()!! }.sort()
        return ErrorMessage.DECLARATIONS_OUT_OF_SCOPE.addAdditionalInfo(declStr)
    }

    return null
}

private fun ExtractionData.getLocalInstructions(pseudocode: Pseudocode): List<Instruction> {
    val instructions = ArrayList<Instruction>()
    pseudocode.traverse(TraversalOrder.FORWARD) {
        if (it is JetElementInstruction && it.element.isInsideOf(originalElements)) {
            instructions.add(it)
        }
    }
    return instructions
}

fun ExtractionData.performAnalysis(): AnalysisResult {
    if (originalElements.empty) {
        return AnalysisResult(null, Status.CRITICAL_ERROR, listOf(ErrorMessage.NO_EXPRESSION))
    }

    val commonParent = PsiTreeUtil.findCommonParent(originalElements)!!

    val enclosingDeclaration = commonParent.getParentByType(javaClass<JetDeclarationWithBody>())
    if (enclosingDeclaration == null) {
        return AnalysisResult(null, Status.CRITICAL_ERROR, listOf(ErrorMessage.NO_CONTAINER))
    }

    val resolveSession = originalFile.getLazyResolveSession()
    val bindingContext = resolveSession.resolveToElement(enclosingDeclaration.getBodyExpression())

    val pseudocode = PseudocodeUtil.generatePseudocode(enclosingDeclaration, bindingContext)
    val localInstructions = getLocalInstructions(pseudocode)

    val replacementMap = HashMap<Int, Replacement>()
    val parameters = HashSet<Parameter>()
    val typeParameters = HashSet<TypeParameter>()
    val nonDenotableTypes = HashSet<JetType>()
    val parameterError = inferParametersInfo(
            commonParent, pseudocode, localInstructions, bindingContext, replacementMap, parameters, typeParameters, nonDenotableTypes
    )
    if (parameterError != null) {
        return AnalysisResult(null, Status.CRITICAL_ERROR, listOf(parameterError))
    }

    val messages = ArrayList<ErrorMessage>()

    val (controlFlow, controlFlowMessage) = localInstructions.analyzeControlFlow(pseudocode, bindingContext, options, parameters)
    controlFlowMessage?.let { messages.add(it) }

    controlFlow.returnType.processTypeIfExtractable(bindingContext, typeParameters, nonDenotableTypes)

    if (nonDenotableTypes.isNotEmpty()) {
        val typeStr = nonDenotableTypes.map {DescriptorRenderer.HTML.renderType(it)}.sort()
        return AnalysisResult(
                null,
                Status.CRITICAL_ERROR,
                listOf(ErrorMessage.DENOTABLE_TYPES.addAdditionalInfo(typeStr))
        )
    }

    checkLocalDeclarationsWithNonLocalUsages(pseudocode, localInstructions, bindingContext)?.let { messages.add(it) }
    checkDeclarationsMovingOutOfScope(controlFlow)?.let { messages.add(it) }

    val functionNameValidator =
            JetNameValidatorImpl(
                    targetSibling.getParent(),
                    targetSibling,
                    JetNameValidatorImpl.Target.FUNCTIONS_AND_CLASSES
            )
    val functionName = JetNameSuggester.suggestNames(controlFlow.returnType, functionNameValidator, DEFAULT_FUNCTION_NAME).first()

    val receiverCandidates = parameters.filterTo(HashSet<Parameter>()) { it.receiverCandidate }
    val receiverParameter = if (receiverCandidates.size == 1) receiverCandidates.first() else null
    receiverParameter?.let { parameters.remove(it) }

    return AnalysisResult(
            ExtractionDescriptor(
                    this,
                    functionName,
                    "",
                    parameters.sortBy { it.name },
                    receiverParameter,
                    typeParameters.sortBy { it.originalDeclaration.getName()!! },
                    replacementMap,
                    controlFlow
            ),
            if (messages.empty) Status.SUCCESS else Status.NON_CRITICAL_ERROR,
            messages
    )
}

fun ExtractionDescriptor.validate(): ExtractionDescriptorWithConflicts {
    val conflicts = MultiMap<PsiElement, String>()

    val nameByOffset = HashMap<Int, JetElement>()
    val function = generateFunction(true, nameByOffset)

    val bindingContext = AnalyzerFacadeWithCache.getContextForElement(function.getBodyExpression()!!)

    for ((originalOffset, resolveResult) in extractionData.refOffsetToDeclaration) {
        if (resolveResult.declaration.isInsideOf(extractionData.originalElements)) continue

        val currentRefExpr = nameByOffset[originalOffset] as JetSimpleNameExpression?
        if (currentRefExpr == null) continue

        if (currentRefExpr.getParent() is JetThisExpression) continue

        val diagnostics = bindingContext.getDiagnostics().forElement(currentRefExpr)

        val currentDescriptor = bindingContext[BindingContext.REFERENCE_TARGET, currentRefExpr]
        val currentTarget = currentDescriptor?.let {
            DescriptorToDeclarationUtil.getDeclaration(extractionData.project, it, bindingContext)
        } as? PsiNamedElement
        if (currentTarget is JetParameter && currentTarget.getParent() == function.getValueParameterList()) continue
        if (currentDescriptor is LocalVariableDescriptor
        && parameters.any { it.mirrorVarName == currentDescriptor.getName().asString() }) continue

        if (diagnostics.any { it.getFactory() == Errors.UNRESOLVED_REFERENCE }
        || (currentDescriptor != null
        && !ErrorUtils.isError(currentDescriptor)
        && !comparePossiblyOverridingDescriptors(currentDescriptor, resolveResult.descriptor))) {
            conflicts.putValue(
                    currentRefExpr,
                    JetRefactoringBundle.message(
                            "0.will.no.longer.be.accessible.after.extraction",
                            RefactoringUIUtil.getDescription(resolveResult.declaration, true)
                    )
            )
            continue
        }

        diagnostics.firstOrNull { it.getFactory() == Errors.INVISIBLE_MEMBER }?.let {
            conflicts.putValue(
                    currentRefExpr,
                    JetRefactoringBundle.message(
                            "0.will.become.invisible.after.extraction",
                            RefactoringUIUtil.getDescription(resolveResult.declaration, true)
                    )
            )
        }
    }

    return ExtractionDescriptorWithConflicts(this, conflicts)
}

private fun comparePossiblyOverridingDescriptors(currentDescriptor: DeclarationDescriptor?, originalDescriptor: DeclarationDescriptor?): Boolean {
    if (compareDescriptors(currentDescriptor, originalDescriptor)) return true
    if (originalDescriptor is CallableDescriptor) {
        if (!OverridingUtil.traverseOverridenDescriptors(originalDescriptor) { !compareDescriptors(currentDescriptor, it) }) return true
    }

    return false
}

fun ExtractionDescriptor.getFunctionText(
        withBody: Boolean = true,
        descriptorRenderer: DescriptorRenderer = DescriptorRenderer.FQ_NAMES_IN_TYPES
): String {
    return FunctionBuilder().let { builder ->
        builder.modifier(visibility)

        builder.typeParams(typeParameters.map { it.originalDeclaration.getText()!! })

        receiverParameter?.let { builder.receiver(descriptorRenderer.renderType(it.parameterType)) }

        builder.name(name)

        parameters.forEach { parameter ->
            builder.param(parameter.name, descriptorRenderer.renderType(parameter.parameterType))
        }

        with(controlFlow.returnType) {
            if (isDefault()) builder.noReturnType() else builder.returnType(descriptorRenderer.renderType(this))
        }

        builder.typeConstraints(typeParameters.flatMap { it.originalConstraints }.map { it.getText()!! })

        if (withBody) {
            builder.blockBody(extractionData.getCodeFragmentText())
        }

        builder.toFunctionText()
    }
}

fun createNameCounterpartMap(from: JetElement, to: JetElement): Map<JetSimpleNameExpression, JetSimpleNameExpression> {
    val map = HashMap<JetSimpleNameExpression, JetSimpleNameExpression>()

    val fromOffset = from.getTextRange()!!.getStartOffset()
    from.accept(
            object: JetTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                    val offset = expression.getTextRange()!!.getStartOffset() - fromOffset
                    val newExpression = to.findElementAt(offset)?.getParentByType(javaClass<JetSimpleNameExpression>())
                    assert(newExpression!= null, "Couldn't find expression at $offset in '${to.getText()}'")

                    map[expression] = newExpression!!
                }
            }
    )

    return map
}

fun ExtractionDescriptor.generateFunction(
        inTempFile: Boolean = false,
        nameByOffset: MutableMap<Int, JetElement> = HashMap()
): JetNamedFunction {
    val project = extractionData.project

    fun createFunction(): JetNamedFunction {
        return with(extractionData) {
            if (inTempFile) {
                createTemporaryFunction("${getFunctionText()}\n")
            }
            else {
                JetPsiFactory.createFunction(project, getFunctionText())
            }
        }
    }

    fun adjustFunctionBody(function: JetNamedFunction) {
        val body = function.getBodyExpression() as JetBlockExpression

        val exprReplacementMap = HashMap<JetElement, (JetElement) -> JetElement>()
        val originalOffsetByExpr = LinkedHashMap<JetElement, Int>()

        val bodyOffset = body.getBlockContentOffset()
        val file = body.getContainingFile()!!

        /*
         * Sort by descending position so that internals of value/type arguments in calls and qualified types are replaced
         * before calls/types themselves
         */
        for ((offsetInBody, resolveResult) in extractionData.refOffsetToDeclaration.entrySet().sortDescendingBy { it.key }) {
            val expr = file.findElementAt(bodyOffset + offsetInBody)?.getParentByType(javaClass<JetSimpleNameExpression>())
            assert(expr != null, "Couldn't find expression at $offsetInBody in '${body.getText()}'")

            originalOffsetByExpr[expr!!] = offsetInBody

            replacementMap[offsetInBody]?.let { replacement ->
                if (replacement !is ParameterReplacement || replacement.parameter != receiverParameter) {
                    exprReplacementMap[expr] = replacement
                }
            }
        }

        val replacingReturn: JetExpression?
        val expressionsToReplaceWithReturn: List<JetElement>
        if (controlFlow is JumpBasedControlFlow) {
            replacingReturn = JetPsiFactory.createExpression(project, if (controlFlow is ConditionalJump) "return true" else "return")
            expressionsToReplaceWithReturn = controlFlow.elementsToReplace.map { jumpElement ->
                val offsetInBody = jumpElement.getTextRange()!!.getStartOffset() - extractionData.originalStartOffset!!
                val expr = file.findElementAt(bodyOffset + offsetInBody)?.getParentByType(jumpElement.javaClass)
                assert(expr != null, "Couldn't find expression at $offsetInBody in '${body.getText()}'")

                expr!!
            }
        }
        else {
            replacingReturn = null
            expressionsToReplaceWithReturn = Collections.emptyList()
        }

        if (replacingReturn != null) {
            for (expr in expressionsToReplaceWithReturn) {
                expr.replace(replacingReturn)
            }
        }

        for ((expr, originalOffset) in originalOffsetByExpr) {
            if (expr.isValid()) {
                nameByOffset.put(originalOffset, exprReplacementMap[expr]?.invoke(expr) ?: expr)
            }
        }

        for (param in parameters) {
            param.mirrorVarName?.let { varName ->
                body.prependElement(JetPsiFactory.createProperty(project, varName, null, true, param.name))
            }
        }

        when (controlFlow) {
            is ParameterUpdate ->
                body.appendElement(JetPsiFactory.createReturn(project, controlFlow.parameter.nameForRef))

            is ConditionalJump ->
                body.appendElement(JetPsiFactory.createReturn(project, "false"))

            is ExpressionEvaluation ->
                body.getStatements().last?.let {
                    val newExpr = it.replaced(JetPsiFactory.createReturn(project, it.getText() ?: throw AssertionError("Return expression shouldn't be empty: code fragment = ${body.getText()}"))).getReturnedExpression()!!
                    val counterpartMap = createNameCounterpartMap(it, newExpr)
                    nameByOffset.entrySet().forEach { e -> counterpartMap[e.getValue()]?.let { e.setValue(it) } }
                }
        }
    }

    fun insertFunction(function: JetNamedFunction): JetNamedFunction {
        return with(extractionData) {
            val targetContainer = targetSibling.getParent()!!
            val emptyLines = JetPsiFactory.createWhiteSpace(project, "\n\n")
            if (insertBefore) {
                val functionInFile = targetContainer.addBefore(function, targetSibling) as JetNamedFunction
                targetContainer.addBefore(emptyLines, targetSibling)

                functionInFile
            }
            else {
                val functionInFile = targetContainer.addAfter(function, targetSibling) as JetNamedFunction
                targetContainer.addAfter(emptyLines, targetSibling)

                functionInFile
            }
        }
    }

    fun insertCall(anchor: PsiElement, wrappedCall: JetExpression) {
        val firstExpression = extractionData.getExpressions().firstOrNull()
        val enclosingCall = firstExpression?.getParent() as? JetCallExpression
        if (enclosingCall == null || firstExpression !in enclosingCall.getFunctionLiteralArguments()) {
            anchor.replace(wrappedCall)
            return
        }

        val argumentListExt = JetPsiFactory.createCallArguments(project, "(${wrappedCall.getText()})")
        val argumentList = enclosingCall.getValueArgumentList()
        if (argumentList == null) {
            (anchor.getPrevSibling() as? PsiWhiteSpace)?.let { it.delete() }
            anchor.replace(argumentListExt)
            return
        }

        val newArgText = (argumentList.getArguments() + argumentListExt.getArguments()).map { it.getText() }.joinToString(", ", "(", ")")
        argumentList.replace(JetPsiFactory.createCallArguments(project, newArgText))
        anchor.delete()
    }

    fun makeCall(function: JetNamedFunction): JetNamedFunction {
        val anchor = extractionData.originalElements.first
        if (anchor == null) return function

        anchor.getNextSibling()?.let { from ->
            val to = extractionData.originalElements.last
            if (to != anchor) {
                anchor.getParent()!!.deleteChildRange(from, to);
            }
        }

        val callText = parameters
                .map { it.argumentText }
                .joinToString(separator = ", ", prefix = "$name(", postfix = ")")
        val wrappedCall = when (controlFlow) {
            is ExpressionEvaluationWithCallSiteReturn ->
                JetPsiFactory.createReturn(project, callText)

            is ParameterUpdate ->
                JetPsiFactory.createExpression(project, "${controlFlow.parameter.argumentText} = $callText")

            is ConditionalJump ->
                JetPsiFactory.createExpression(project, "if ($callText) ${controlFlow.elementToInsertAfterCall.getText()}")

            is UnconditionalJump -> {
                val anchorParent = anchor.getParent()!!
                anchorParent.addAfter(
                        JetPsiFactory.createExpression(project, controlFlow.elementToInsertAfterCall.getText()),
                        anchor
                )
                anchorParent.addAfter(JetPsiFactory.createNewLine(project), anchor)

                JetPsiFactory.createExpression(project, callText)
            }

            else ->
                JetPsiFactory.createExpression(project, callText)
        }
        insertCall(anchor, wrappedCall)

        return function
    }

    val function = createFunction()
    adjustFunctionBody(function)

    if (inTempFile) return function

    val functionInPlace = makeCall(insertFunction(function))
    ShortenReferences.process(functionInPlace)
    return functionInPlace
}