/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg.pseudocode.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitorWithResult
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionWithNext
import org.jetbrains.kotlin.cfg.pseudocode.instructions.JetElementInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.MarkInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverseFollowingInstructions
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.comparePossiblyOverridingDescriptors
import org.jetbrains.kotlin.idea.core.getResolutionScope
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.refactoring.createTempCopy
import org.jetbrains.kotlin.idea.core.refactoring.getContextForContainingDeclarationBody
import org.jetbrains.kotlin.idea.imports.importableFqNameSafe
import org.jetbrains.kotlin.idea.kdoc.getResolutionScope
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.ErrorMessage
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.Status
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.ExpressionValue
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.Initializer
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.Jump
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.ParameterUpdate
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValueBoxer.AsList
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.approximateWithResolvableType
import org.jetbrains.kotlin.idea.util.isResolvableInScope
import org.jetbrains.kotlin.idea.util.makeNullable
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.isSynthesizedInvoke
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.JetScopeUtils
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.DFS.CollectingNodeHandler
import org.jetbrains.kotlin.utils.DFS.Neighbors
import org.jetbrains.kotlin.utils.DFS.VisitedWithSet
import java.util.*
import kotlin.properties.Delegates

private val DEFAULT_RETURN_TYPE = KotlinBuiltIns.getInstance().getUnitType()
private val DEFAULT_PARAMETER_TYPE = KotlinBuiltIns.getInstance().getNullableAnyType()

private fun DeclarationDescriptor.renderForMessage(): String =
        IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(this)

private fun JetType.renderForMessage(): String =
        IdeDescriptorRenderers.SOURCE_CODE.renderType(this)

private fun JetDeclaration.renderForMessage(bindingContext: BindingContext): String? =
    bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this]?.renderForMessage()

private fun JetType.isDefault(): Boolean = KotlinBuiltIns.isUnit(this)

private fun List<Instruction>.getModifiedVarDescriptors(bindingContext: BindingContext): Map<VariableDescriptor, List<JetExpression>> {
    val result = HashMap<VariableDescriptor, MutableList<JetExpression>>()
    for (instruction in filterIsInstance<WriteValueInstruction>()) {
        val expression = instruction.element as? JetExpression
        val descriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
        if (expression != null && descriptor != null) {
            result.getOrPut(descriptor) { ArrayList() }.add(expression)
        }
    }

    return result
}

private fun List<Instruction>.getVarDescriptorsAccessedAfterwards(bindingContext: BindingContext): Set<VariableDescriptor> {
    val accessedAfterwards = HashSet<VariableDescriptor>()
    val visitedInstructions = HashSet<Instruction>()

    fun doTraversal(instruction: Instruction) {
        traverseFollowingInstructions(instruction, visitedInstructions, TraversalOrder.FORWARD) {
            when {
                it is AccessValueInstruction && it !in this ->
                    PseudocodeUtil.extractVariableDescriptorIfAny(it, false, bindingContext)?.let { accessedAfterwards.add(it) }

                it is LocalFunctionDeclarationInstruction ->
                    doTraversal(it.body.getEnterInstruction())
            }

            true
        }
    }

    forEach(::doTraversal)
    return accessedAfterwards
}

private fun List<Instruction>.getExitPoints(): List<Instruction> =
        filter { localInstruction -> localInstruction.nextInstructions.any { it !in this } }

private fun List<Instruction>.getResultTypeAndExpressions(
        bindingContext: BindingContext,
        targetScope: JetScope?,
        options: ExtractionOptions
): Pair<JetType, List<JetExpression>> {
    fun instructionToExpression(instruction: Instruction, unwrapReturn: Boolean): JetExpression? {
        return when (instruction) {
            is ReturnValueInstruction ->
                (if (unwrapReturn) null else instruction.returnExpressionIfAny) ?: instruction.returnedValue.element as? JetExpression
            is InstructionWithValue ->
                instruction.outputValue?.element as? JetExpression
            else -> null
        }
    }

    fun instructionToType(instruction: Instruction): JetType? {
        val expression = instructionToExpression(instruction, true)

        if (expression == null) return null
        if (options.inferUnitTypeForUnusedValues && expression.isUsedAsStatement(bindingContext)) return null

        return bindingContext.getType(expression)
               ?: (expression as? JetReferenceExpression)?.let {
                   (bindingContext[BindingContext.REFERENCE_TARGET, it] as? CallableDescriptor)?.getReturnType()
               }
    }

    val resultTypes = map(::instructionToType).filterNotNull()
    var commonSupertype = if (resultTypes.isNotEmpty()) CommonSupertypes.commonSupertype(resultTypes) else DEFAULT_RETURN_TYPE
    val resultType = if (options.allowSpecialClassNames) commonSupertype else commonSupertype.approximateWithResolvableType(targetScope, false)

    val expressions = map { instructionToExpression(it, false) }.filterNotNull()

    return resultType to expressions
}

private fun getCommonNonTrivialSuccessorIfAny(instructions: List<Instruction>): Instruction? {
    val singleSuccessorCheckingVisitor = object: InstructionVisitorWithResult<Boolean>() {
        var target: Instruction? = null

        override fun visitInstructionWithNext(instruction: InstructionWithNext): Boolean {
            return when (instruction) {
                is LoadUnitValueInstruction,
                is MergeInstruction,
                is MarkInstruction -> {
                    instruction.next?.accept(this) ?: true
                }
                else -> visitInstruction(instruction)
            }
        }

        override fun visitJump(instruction: AbstractJumpInstruction): Boolean {
            return when (instruction) {
                is ConditionalJumpInstruction -> visitInstruction(instruction)
                else -> instruction.resolvedTarget?.accept(this) ?: true
            }
        }

        override fun visitInstruction(instruction: Instruction): Boolean {
            if (target != null && target != instruction) return false
            target = instruction
            return true
        }
    }

    if (instructions.flatMap { it.nextInstructions }.any { !it.accept(singleSuccessorCheckingVisitor) }) return null
    return singleSuccessorCheckingVisitor.target ?: instructions.firstOrNull()?.owner?.getSinkInstruction()
}

private fun JetType.isMeaningful(): Boolean {
    return !KotlinBuiltIns.isUnit(this) && !KotlinBuiltIns.isNothing(this)
}

private fun ExtractionData.getLocalDeclarationsWithNonLocalUsages(
        pseudocode: Pseudocode,
        localInstructions: List<Instruction>,
        bindingContext: BindingContext
): List<JetNamedDeclaration> {
    val declarations = HashSet<JetNamedDeclaration>()
    pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
        if (instruction !in localInstructions) {
            instruction.getPrimaryDeclarationDescriptorIfAny(bindingContext)?.let { descriptor ->
                val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
                if (declaration is JetNamedDeclaration && declaration.isInsideOf(originalElements)) {
                    declarations.add(declaration)
                }
            }
        }
    }
    return declarations.sortBy { it.getTextRange()!!.getStartOffset() }
}

private fun ExtractionData.analyzeControlFlow(
        localInstructions: List<Instruction>,
        pseudocode: Pseudocode,
        module: ModuleDescriptor,
        bindingContext: BindingContext,
        modifiedVarDescriptors: Map<VariableDescriptor, List<JetExpression>>,
        options: ExtractionOptions,
        targetScope: JetScope?,
        parameters: Set<Parameter>
): Pair<ControlFlow, ErrorMessage?> {
    val exitPoints = localInstructions.getExitPoints()

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
            is ReturnValueInstruction -> {
                if (insn.owner == pseudocode) {
                    if (insn.returnExpressionIfAny == null) {
                        defaultExits.add(insn)
                    }
                    else {
                        valuedReturnExits.add(insn)
                    }
                }
            }

            is AbstractJumpInstruction -> {
                val element = insn.element
                if ((element is JetReturnExpression && insn.owner == pseudocode)
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

    val nonLocallyUsedDeclarations = getLocalDeclarationsWithNonLocalUsages(pseudocode, localInstructions, bindingContext)
    val (declarationsToCopy, declarationsToReport) = nonLocallyUsedDeclarations.partition { it is JetProperty && it.isLocal() }

    val (typeOfDefaultFlow, defaultResultExpressions) = defaultExits.getResultTypeAndExpressions(bindingContext, targetScope, options)
    val (returnValueType, valuedReturnExpressions) = valuedReturnExits.getResultTypeAndExpressions(bindingContext, targetScope, options)

    val emptyControlFlow =
            ControlFlow(Collections.emptyList(), { OutputValueBoxer.AsTuple(it, module) }, declarationsToCopy)

    val defaultReturnType = if (returnValueType.isMeaningful()) returnValueType else typeOfDefaultFlow
    if (defaultReturnType.isError()) return emptyControlFlow to ErrorMessage.ERROR_TYPES

    val controlFlow = if (defaultReturnType.isMeaningful()) {
        emptyControlFlow.copy(outputValues = Collections.singletonList(ExpressionValue(false, defaultResultExpressions, defaultReturnType)))
    }
    else {
        emptyControlFlow
    }

    if (declarationsToReport.isNotEmpty()) {
        val localVarStr = declarationsToReport.map { it.renderForMessage(bindingContext)!! }.distinct().sort()
        return controlFlow to ErrorMessage.DECLARATIONS_ARE_USED_OUTSIDE.addAdditionalInfo(localVarStr)
    }

    val outParameters =
            parameters.filter { it.mirrorVarName != null && modifiedVarDescriptors[it.originalDescriptor] != null }.sortBy { it.nameForRef }
    val outDeclarations =
            declarationsToCopy.filter { modifiedVarDescriptors[bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it]] != null }
    val modifiedValueCount = outParameters.size() + outDeclarations.size()

    val outputValues = ArrayList<OutputValue>()

    val multipleExitsError = controlFlow to ErrorMessage.MULTIPLE_EXIT_POINTS
    val outputAndExitsError = controlFlow to ErrorMessage.OUTPUT_AND_EXIT_POINT

    if (typeOfDefaultFlow.isMeaningful()) {
        if (valuedReturnExits.isNotEmpty() || jumpExits.isNotEmpty()) return multipleExitsError

        outputValues.add(ExpressionValue(false, defaultResultExpressions, typeOfDefaultFlow))
    }
    else if (valuedReturnExits.isNotEmpty()) {
        if (jumpExits.isNotEmpty()) return multipleExitsError

        if (defaultExits.isNotEmpty()) {
            if (modifiedValueCount != 0) return outputAndExitsError
            if (valuedReturnExits.size() != 1) return multipleExitsError

            val element = valuedReturnExits.first().element as JetExpression
            return controlFlow.copy(outputValues = Collections.singletonList(Jump(listOf(element), element, true))) to null
        }

        if (getCommonNonTrivialSuccessorIfAny(valuedReturnExits) == null) return multipleExitsError
        outputValues.add(ExpressionValue(true, valuedReturnExpressions, returnValueType))
    }

    outDeclarations.mapTo(outputValues) {
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it] as? CallableDescriptor
        Initializer(it as JetProperty, descriptor?.getReturnType() ?: DEFAULT_PARAMETER_TYPE)
    }
    outParameters.mapTo(outputValues) { ParameterUpdate(it, modifiedVarDescriptors[it.originalDescriptor]!!) }

    if (outputValues.isNotEmpty()) {
        if (jumpExits.isNotEmpty()) return outputAndExitsError

        val boxerFactory: (List<OutputValue>) -> OutputValueBoxer = when {
            outputValues.size() > 3 -> {
                if (!options.enableListBoxing) {
                    val outValuesStr =
                            (outParameters.map { it.originalDescriptor.renderForMessage() }
                             + outDeclarations.map { it.renderForMessage(bindingContext)!! }).sort()
                    return controlFlow to ErrorMessage.MULTIPLE_OUTPUT.addAdditionalInfo(outValuesStr)
                }
                OutputValueBoxer::AsList
            }

            else -> controlFlow.boxerFactory
        }

        return controlFlow.copy(outputValues = outputValues, boxerFactory = boxerFactory) to null
    }

    if (jumpExits.isNotEmpty()) {
        val jumpTarget = getCommonNonTrivialSuccessorIfAny(jumpExits)
        if (jumpTarget == null) return multipleExitsError

        val singleExit = getCommonNonTrivialSuccessorIfAny(defaultExits) == jumpTarget
        val conditional = !singleExit && defaultExits.isNotEmpty()
        val elements = jumpExits.map { it.element as JetExpression }
        val elementToInsertAfterCall = if (singleExit) null else elements.first()
        return controlFlow.copy(outputValues = Collections.singletonList(Jump(elements, elementToInsertAfterCall, conditional))) to null
    }

    return controlFlow to null
}

fun ExtractionData.createTemporaryDeclaration(functionText: String): JetNamedDeclaration {
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
    return tmpFile.findElementAt(lookupPosition)?.getNonStrictParentOfType<JetNamedDeclaration>()!!
}

private fun ExtractionData.createTemporaryCodeBlock(): JetBlockExpression =
        (createTemporaryDeclaration("fun() {\n$codeFragmentText\n}\n") as JetNamedFunction).getBodyExpression() as JetBlockExpression

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
    val typeConstraints = getNonStrictParentOfType<JetTypeParameterListOwner>()?.getTypeConstraints()
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

private fun JetType.isExtractable(targetScope: JetScope?): Boolean {
    return collectReferencedTypes(true).fold(true) { extractable, typeToCheck ->
        val parameterTypeDescriptor = typeToCheck.getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor
        val typeParameter = parameterTypeDescriptor?.let {
            DescriptorToSourceUtils.descriptorToDeclaration(it)
        } as? JetTypeParameter

        extractable && (typeParameter != null || typeToCheck.isResolvableInScope(targetScope, false))
    }
}

private fun JetType.processTypeIfExtractable(
        typeParameters: MutableSet<TypeParameter>,
        nonDenotableTypes: MutableSet<JetType>,
        options: ExtractionOptions,
        targetScope: JetScope?,
        processTypeArguments: Boolean = true
): Boolean {
    return collectReferencedTypes(processTypeArguments).fold(true) { extractable, typeToCheck ->
        val parameterTypeDescriptor = typeToCheck.getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor
        val typeParameter = parameterTypeDescriptor?.let {
            DescriptorToSourceUtils.descriptorToDeclaration(it)
        } as? JetTypeParameter

        when {
            typeToCheck.isResolvableInScope(targetScope, true) ->
                extractable

            typeParameter != null -> {
                typeParameters.add(TypeParameter(typeParameter, typeParameter.collectRelevantConstraints()))
                extractable
            }

            options.allowSpecialClassNames && typeToCheck.isSpecial() ->
                extractable

            typeToCheck.isError() ->
                false

            else -> {
                nonDenotableTypes.add(typeToCheck)
                false
            }
        }
    }
}

private class MutableParameter(
        override val argumentText: String,
        override val originalDescriptor: DeclarationDescriptor,
        override val receiverCandidate: Boolean,
        private val targetScope: JetScope?
): Parameter {
    // All modifications happen in the same thread
    private var writable: Boolean = true
    private val defaultTypes = HashSet<JetType>()
    private val typePredicates = HashSet<TypePredicate>()

    var refCount: Int = 0

    fun addDefaultType(jetType: JetType) {
        assert(writable) { "Can't add type to non-writable parameter $currentName" }
        defaultTypes.add(jetType)
    }

    fun addTypePredicate(predicate: TypePredicate) {
        assert(writable) { "Can't add type predicate to non-writable parameter $currentName" }
        typePredicates.add(predicate)
    }

    var currentName: String? = null
    override val name: String get() = currentName!!

    override var mirrorVarName: String? = null

    private val defaultType: JetType by Delegates.lazy {
        writable = false
        TypeUtils.intersect(JetTypeChecker.DEFAULT, defaultTypes)!!
    }

    private val parameterTypeCandidates: List<JetType> by Delegates.lazy {
        writable = false

        val typePredicate = and(typePredicates)

        val typeList = if (defaultType.isNullabilityFlexible()) {
            val bounds = defaultType.getCapability(javaClass<Flexibility>())
            if (typePredicate(bounds!!.upperBound)) arrayListOf(bounds.upperBound, bounds.lowerBound) else arrayListOf(bounds.lowerBound)
        }
        else arrayListOf(defaultType)

        val addNullableTypes = typeList.size() > 1
        val superTypes = TypeUtils.getAllSupertypes(defaultType).filter(typePredicate)

        for (superType in superTypes) {
            if (addNullableTypes) {
                typeList.add(superType.makeNullable())
            }
            typeList.add(superType)
        }

        typeList
    }

    override fun getParameterTypeCandidates(allowSpecialClassNames: Boolean): List<JetType> {
            return if (!allowSpecialClassNames) {
                parameterTypeCandidates.filter { it.isExtractable(targetScope) }
            } else {
                parameterTypeCandidates
            }
    }

    override fun getParameterType(allowSpecialClassNames: Boolean): JetType {
        return getParameterTypeCandidates(allowSpecialClassNames).firstOrNull() ?: defaultType
    }

    override fun copy(name: String, parameterType: JetType): Parameter = DelegatingParameter(this, name, parameterType)
}

private class DelegatingParameter(
        val original: Parameter,
        override val name: String,
        val parameterType: JetType
): Parameter by original {
    override fun copy(name: String, parameterType: JetType): Parameter = DelegatingParameter(original, name, parameterType)
    override fun getParameterType(allowSpecialClassNames: Boolean) = parameterType
}

private class ParametersInfo {
    var errorMessage: ErrorMessage? = null
    val replacementMap: MutableMap<Int, Replacement> = HashMap()
    val originalRefToParameter: MutableMap<JetSimpleNameExpression, MutableParameter> = HashMap()
    val parameters: MutableSet<MutableParameter> = HashSet()
    val typeParameters: MutableSet<TypeParameter> = HashSet()
    val nonDenotableTypes: MutableSet<JetType> = HashSet()
}

private fun ExtractionData.inferParametersInfo(
        commonParent: PsiElement,
        pseudocode: Pseudocode,
        bindingContext: BindingContext,
        targetScope: JetScope?,
        modifiedVarDescriptors: Set<VariableDescriptor>
): ParametersInfo {
    val info = ParametersInfo()

    val extractedDescriptorToParameter = HashMap<DeclarationDescriptor, MutableParameter>()

    for (refInfo in getBrokenReferencesInfo(createTemporaryCodeBlock())) {
        val (originalRef, originalDeclaration, originalDescriptor, resolvedCall) = refInfo.resolveResult
        val ref = refInfo.refExpr

        val selector = (ref.getParent() as? JetCallExpression) ?: ref
        val superExpr = (selector.getParent() as? JetQualifiedExpression)?.getReceiverExpression() as? JetSuperExpression
        if (superExpr != null) {
            info.errorMessage = ErrorMessage.SUPER_CALL
            return info
        }

        val extensionReceiver = resolvedCall?.getExtensionReceiver()
        val receiverToExtract = when {
                           extensionReceiver == ReceiverValue.NO_RECEIVER,
                           isSynthesizedInvoke(originalDescriptor) -> resolvedCall?.getDispatchReceiver()
                           else -> extensionReceiver
                       } ?: ReceiverValue.NO_RECEIVER

        val thisDescriptor = (receiverToExtract as? ThisReceiver)?.getDeclarationDescriptor()
        val hasThisReceiver = thisDescriptor != null
        val thisExpr = ref.getParent() as? JetThisExpression

        if (hasThisReceiver
            && DescriptorToSourceUtilsIde.getAllDeclarations(project, thisDescriptor!!).all { it.isInsideOf(originalElements) }) {
            continue
        }

        val referencedClassDescriptor: ClassDescriptor? = (thisDescriptor ?: originalDescriptor).let {
            when (it) {
                is ClassDescriptor ->
                    when(it.getKind()) {
                        ClassKind.OBJECT, ClassKind.ENUM_CLASS -> it : ClassDescriptor
                        ClassKind.ENUM_ENTRY -> it.getContainingDeclaration() as? ClassDescriptor
                        else -> if (ref.getNonStrictParentOfType<JetTypeReference>() != null) it : ClassDescriptor else null
                    }

                is ConstructorDescriptor -> it.getContainingDeclaration()

                else -> null
            }
        }

        if (referencedClassDescriptor != null) {
            if (!referencedClassDescriptor.getDefaultType().processTypeIfExtractable(
                    info.typeParameters, info.nonDenotableTypes, options, targetScope, false
            )) continue

            info.replacementMap[refInfo.offsetInBody] = FqNameReplacement(originalDescriptor.importableFqNameSafe)
        }
        else {
            val extractThis = (hasThisReceiver && refInfo.smartCast == null) || thisExpr != null
            val extractOrdinaryParameter =
                    originalDeclaration is JetMultiDeclarationEntry ||
                            originalDeclaration is JetProperty ||
                            originalDeclaration is JetParameter

            val extractFunctionRef =
                    options.captureLocalFunctions
                    && originalRef.getReferencedName() == originalDescriptor.getName().asString() // to forbid calls by convention
                    && originalDeclaration is JetNamedFunction && originalDeclaration.isLocal()
                    && (targetScope == null || originalDescriptor !in targetScope.getFunctions(originalDescriptor.getName()))

            val descriptorToExtract = (if (extractThis) thisDescriptor else null) ?: originalDescriptor

            val extractParameter = extractThis || extractOrdinaryParameter || extractFunctionRef
            if (extractParameter) {
                val parameterExpression = when {
                    receiverToExtract is ExpressionReceiver -> {
                        val receiverExpression = receiverToExtract.getExpression()
                        // If p.q has a smart-cast, then extract entire qualified expression
                        if (refInfo.smartCast != null) receiverExpression.getParent() as JetExpression else receiverExpression
                    }
                    receiverToExtract.exists() && refInfo.smartCast == null -> null
                    else -> (originalRef.getParent() as? JetThisExpression) ?: originalRef
                }

                val parameterType = when {
                    extractFunctionRef -> {
                        originalDescriptor as FunctionDescriptor
                        KotlinBuiltIns.getInstance().getFunctionType(Annotations.EMPTY,
                                                                     originalDescriptor.getExtensionReceiverParameter()?.getType(),
                                                                     originalDescriptor.getValueParameters().map { it.getType() },
                                                                     originalDescriptor.getReturnType() ?: DEFAULT_RETURN_TYPE)
                    }
                    parameterExpression != null ->
                        bindingContext[BindingContext.SMARTCAST, parameterExpression]
                        ?: bindingContext.getType(parameterExpression)
                        ?: (parameterExpression as? JetReferenceExpression)?.let {
                            (bindingContext[BindingContext.REFERENCE_TARGET, it] as? CallableDescriptor)?.getReturnType()
                        }
                        ?: if (receiverToExtract.exists()) receiverToExtract.getType() else null
                    receiverToExtract is ThisReceiver -> {
                        val calleeExpression = resolvedCall!!.getCall().getCalleeExpression()
                        bindingContext[BindingContext.EXPRESSION_TYPE_INFO, calleeExpression]?.dataFlowInfo?.let { dataFlowInfo ->
                            val possibleTypes = dataFlowInfo.getPossibleTypes(DataFlowValueFactory.createDataFlowValue(receiverToExtract))
                            if (possibleTypes.isNotEmpty()) CommonSupertypes.commonSupertype(possibleTypes) else null
                        } ?: receiverToExtract.getType()
                    }
                    receiverToExtract.exists() -> receiverToExtract.getType()
                    else -> null
                } ?: DEFAULT_PARAMETER_TYPE

                val parameter = extractedDescriptorToParameter.getOrPut(descriptorToExtract) {
                    var argumentText =
                            if (hasThisReceiver && extractThis) {
                                val label = if (descriptorToExtract is ClassDescriptor) "@${descriptorToExtract.getName().asString()}" else ""
                                "this$label"
                            }
                            else {
                                val argumentExpr = (thisExpr ?: ref).getQualifiedExpressionForSelectorOrThis()
                                argumentExpr.getText() ?: throw AssertionError("'this' reference shouldn't be empty: code fragment = $codeFragmentText")
                            }
                    if (extractFunctionRef) {
                        val receiverTypeText = (originalDeclaration as JetCallableDeclaration).getReceiverTypeReference()?.getText() ?: ""
                        argumentText = "$receiverTypeText::$argumentText"
                    }

                    MutableParameter(argumentText, descriptorToExtract, extractThis, targetScope)
                }

                if (!extractThis) {
                    parameter.currentName = originalDeclaration.getNameIdentifier()?.getText()
                }

                parameter.refCount++
                info.originalRefToParameter[originalRef] = parameter

                parameter.addDefaultType(parameterType)

                if (extractThis && thisExpr == null) {
                    val callElement = resolvedCall!!.getCall().getCallElement()
                    val instruction = pseudocode.getElementValue(callElement)?.createdAt as? InstructionWithReceivers
                    val receiverValue = instruction?.receiverValues?.entrySet()?.singleOrNull { it.getValue() == receiverToExtract }?.getKey()
                    if (receiverValue != null) {
                        parameter.addTypePredicate(getExpectedTypePredicate(receiverValue, bindingContext))
                    }
                }
                else if (extractFunctionRef) {
                    parameter.addTypePredicate(SingleType(parameterType))
                }
                else {
                    pseudocode.getElementValuesRecursively(originalRef).forEach {
                        parameter.addTypePredicate(getExpectedTypePredicate(it, bindingContext))
                    }
                }

                info.replacementMap[refInfo.offsetInBody] =
                        if (hasThisReceiver && extractThis) AddPrefixReplacement(parameter) else RenameReplacement(parameter)
            }
        }
    }

    val varNameValidator = NewDeclarationNameValidator(
            commonParent.getNonStrictParentOfType<JetExpression>()!!,
            originalElements.firstOrNull(),
            NewDeclarationNameValidator.Target.VARIABLES
    )

    for ((descriptorToExtract, parameter) in extractedDescriptorToParameter) {
        if (!parameter
                .getParameterType(options.allowSpecialClassNames)
                .processTypeIfExtractable(info.typeParameters, info.nonDenotableTypes, options, targetScope)) continue

        with (parameter) {
            if (currentName == null) {
                currentName = KotlinNameSuggester.suggestNamesByType(getParameterType(options.allowSpecialClassNames), varNameValidator, "p").first()
            }
            mirrorVarName = if (descriptorToExtract in modifiedVarDescriptors) KotlinNameSuggester.suggestNameByName(name, varNameValidator) else null
            info.parameters.add(this)
        }
    }

    for (typeToCheck in info.typeParameters.flatMapTo(HashSet<JetType>()) { it.collectReferencedTypes(bindingContext) }) {
        typeToCheck.processTypeIfExtractable(info.typeParameters, info.nonDenotableTypes, options, targetScope)
    }


    return info
}

private fun ExtractionData.checkDeclarationsMovingOutOfScope(
        enclosingDeclaration: JetDeclaration,
        controlFlow: ControlFlow,
        bindingContext: BindingContext
): ErrorMessage? {
    val declarationsOutOfScope = HashSet<JetNamedDeclaration>()
    controlFlow.jumpOutputValue?.elementToInsertAfterCall?.accept(
            object : JetTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                    val target = expression.getReference()?.resolve()
                    if (target is JetNamedDeclaration
                        && target.isInsideOf(originalElements)
                        && target.getStrictParentOfType<JetDeclaration>() == enclosingDeclaration) {
                        declarationsOutOfScope.add(target)
                    }
                }
            }
    )

    if (declarationsOutOfScope.isNotEmpty()) {
        val declStr = declarationsOutOfScope.map { it.renderForMessage(bindingContext)!! }.sort()
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

fun ExtractionData.isVisibilityApplicable(): Boolean {
    return when (targetSibling.getParent()) {
        is JetClassBody, is JetFile -> true
        else -> false
    }
}

fun ExtractionData.getDefaultVisibility(): String {
    if (!isVisibilityApplicable()) return ""

    val parent = targetSibling.getStrictParentOfType<JetDeclaration>()
    if (parent is JetClass && parent.isInterface()) return ""

    return "private"
}

fun ExtractionData.performAnalysis(): AnalysisResult {
    if (originalElements.isEmpty()) return AnalysisResult(null, Status.CRITICAL_ERROR, listOf(ErrorMessage.NO_EXPRESSION))

    val noContainerError = AnalysisResult(null, Status.CRITICAL_ERROR, listOf(ErrorMessage.NO_CONTAINER))

    val bindingContext = bindingContext ?: return noContainerError

    val pseudocode = commonParent.getContainingPseudocode(bindingContext) ?: return noContainerError
    val localInstructions = getLocalInstructions(pseudocode)

    val modifiedVarDescriptorsWithExpressions = localInstructions.getModifiedVarDescriptors(bindingContext)

    val targetScope = targetSibling.getResolutionScope(bindingContext, commonParent.getResolutionFacade())
    val paramsInfo = inferParametersInfo(commonParent, pseudocode, bindingContext, targetScope, modifiedVarDescriptorsWithExpressions.keySet())
    if (paramsInfo.errorMessage != null) {
        return AnalysisResult(null, Status.CRITICAL_ERROR, listOf(paramsInfo.errorMessage!!))
    }

    val messages = ArrayList<ErrorMessage>()

    val modifiedVarDescriptorsForControlFlow = HashMap(modifiedVarDescriptorsWithExpressions)
    modifiedVarDescriptorsForControlFlow.keySet().retainAll(localInstructions.getVarDescriptorsAccessedAfterwards(bindingContext))
    val (controlFlow, controlFlowMessage) =
            analyzeControlFlow(
                    localInstructions,
                    pseudocode,
                    originalFile.findModuleDescriptor(),
                    bindingContext,
                    modifiedVarDescriptorsForControlFlow,
                    options,
                    targetScope,
                    paramsInfo.parameters
            )
    controlFlowMessage?.let { messages.add(it) }

    val returnType = controlFlow.outputValueBoxer.returnType
    returnType.processTypeIfExtractable(paramsInfo.typeParameters, paramsInfo.nonDenotableTypes, options, targetScope)

    if (paramsInfo.nonDenotableTypes.isNotEmpty()) {
        val typeStr = paramsInfo.nonDenotableTypes.map {it.renderForMessage()}.sort()
        return AnalysisResult(
                null,
                Status.CRITICAL_ERROR,
                listOf(ErrorMessage.DENOTABLE_TYPES.addAdditionalInfo(typeStr))
        )
    }

    val enclosingDeclaration = commonParent.getStrictParentOfType<JetDeclaration>()!!
    checkDeclarationsMovingOutOfScope(enclosingDeclaration, controlFlow, bindingContext)?.let { messages.add(it) }

    controlFlow.jumpOutputValue?.elementToInsertAfterCall?.accept(
            object : JetTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                    paramsInfo.originalRefToParameter[expression]?.let { it.refCount-- }
                }
            }
    )
    val adjustedParameters = paramsInfo.parameters.filterTo(HashSet<Parameter>()) { it.refCount > 0 }

    val receiverCandidates = adjustedParameters.filterTo(HashSet<Parameter>()) { it.receiverCandidate }
    val receiverParameter = if (receiverCandidates.size() == 1) receiverCandidates.first() else null
    receiverParameter?.let { adjustedParameters.remove(it) }

    return AnalysisResult(
            ExtractableCodeDescriptor(
                    this,
                    bindingContext,
                    suggestFunctionNames(returnType),
                    getDefaultVisibility(),
                    adjustedParameters.sortBy { it.name },
                    receiverParameter,
                    paramsInfo.typeParameters.sortBy { it.originalDeclaration.getName()!! },
                    paramsInfo.replacementMap,
                    if (messages.isEmpty()) controlFlow else controlFlow.toDefault(),
                    returnType
            ),
            if (messages.isEmpty()) Status.SUCCESS else Status.NON_CRITICAL_ERROR,
            messages
    )
}

private fun ExtractionData.suggestFunctionNames(returnType: JetType): List<String> {
    val functionNames = LinkedHashSet<String>()

    val validator =
            NewDeclarationNameValidator(
                    targetSibling.getParent(),
                    if (targetSibling is JetClassInitializer) targetSibling.getParent() else targetSibling,
                    if (options.extractAsProperty) NewDeclarationNameValidator.Target.VARIABLES else NewDeclarationNameValidator.Target.FUNCTIONS_AND_CLASSES
            )
    if (!returnType.isDefault()) {
        functionNames.addAll(KotlinNameSuggester.suggestNamesByType(returnType, validator))
    }

    getExpressions().singleOrNull()?.let { expr ->
        val property = expr.getStrictParentOfType<JetProperty>()
        if (property?.getInitializer() == expr) {
            property?.getName()?.let { functionNames.add(KotlinNameSuggester.suggestNameByName("get" + it.capitalize(), validator)) }
        }
    }

    return functionNames.toList()
}

private fun JetNamedDeclaration.getGeneratedBody() =
        when (this) {
            is JetNamedFunction -> getBodyExpression()
            else -> {
                val property = this as JetProperty

                property.getGetter()?.getBodyExpression()?.let { return it }
                property.getInitializer()?.let { return it }
                // We assume lazy property here with delegate expression 'by Delegates.lazy { body }'
                property.getDelegateExpression()?.let {
                    val call = it.getCalleeExpressionIfAny()?.getParent() as? JetCallExpression
                    call?.getFunctionLiteralArguments()?.singleOrNull()?.getFunctionLiteral()?.getBodyExpression()
                }
            }
        } ?: throw AssertionError("Couldn't get block body for this declaration: ${getElementTextWithContext()}")

fun ExtractableCodeDescriptor.validate(): ExtractableCodeDescriptorWithConflicts {
    fun getDeclarationMessage(declaration: PsiNamedElement, messageKey: String, capitalize: Boolean = true): String {
        val message = JetRefactoringBundle.message(messageKey, RefactoringUIUtil.getDescription(declaration, true))
        return if (capitalize) message.capitalize() else message
    }

    val conflicts = MultiMap<PsiElement, String>()

    val result = ExtractionGeneratorConfiguration(
            this,
            ExtractionGeneratorOptions(inTempFile = true, allowExpressionBody = false)
    ).generateDeclaration()

    val valueParameterList = (result.declaration as? JetNamedFunction)?.getValueParameterList()
    val body = result.declaration.getGeneratedBody()
    val bindingContext = body.analyzeFully()

    fun validateBody() {
        for ((originalOffset, resolveResult) in extractionData.refOffsetToDeclaration) {
            if (resolveResult.declaration.isInsideOf(extractionData.originalElements)) continue

            val currentRefExpr = result.nameByOffset[originalOffset] as JetSimpleNameExpression?
            if (currentRefExpr == null) continue

            if (currentRefExpr.getParent() is JetThisExpression) continue

            val diagnostics = bindingContext.getDiagnostics().forElement(currentRefExpr)

            val currentDescriptor = bindingContext[BindingContext.REFERENCE_TARGET, currentRefExpr]
            val currentTarget =
                    currentDescriptor?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(extractionData.project, it) } as? PsiNamedElement
            if (currentTarget is JetParameter && currentTarget.getParent() == valueParameterList) continue
            if (currentDescriptor is LocalVariableDescriptor
                && parameters.any { it.mirrorVarName == currentDescriptor.getName().asString() }) continue

            if (diagnostics.any { it.getFactory() == Errors.UNRESOLVED_REFERENCE }
                || (currentDescriptor != null
                    && !ErrorUtils.isError(currentDescriptor)
                    && !comparePossiblyOverridingDescriptors(extractionData.project, currentDescriptor, resolveResult.descriptor))) {
                conflicts.putValue(
                        resolveResult.originalRefExpr,
                        getDeclarationMessage(resolveResult.declaration, "0.will.no.longer.be.accessible.after.extraction")
                )
                continue
            }

            diagnostics.firstOrNull { it.getFactory() in Errors.INVISIBLE_REFERENCE_DIAGNOSTICS }?.let {
                val message = when (it.getFactory()) {
                    Errors.INVISIBLE_SETTER ->
                        getDeclarationMessage(resolveResult.declaration, "setter.of.0.will.become.invisible.after.extraction", false)
                    else ->
                        getDeclarationMessage(resolveResult.declaration, "0.will.become.invisible.after.extraction")
                }
                conflicts.putValue(resolveResult.originalRefExpr, message)
            }
        }
    }

    result.declaration.accept(
            object : JetTreeVisitorVoid() {
                override fun visitUserType(userType: JetUserType) {
                    val refExpr = userType.getReferenceExpression() ?: return
                    val declaration = refExpr.getReference()?.resolve() as? PsiNamedElement ?: return
                    val diagnostics = bindingContext.getDiagnostics().forElement(refExpr)
                    diagnostics.firstOrNull { it.getFactory() == Errors.INVISIBLE_REFERENCE }?.let {
                        conflicts.putValue(declaration, getDeclarationMessage(declaration, "0.will.become.invisible.after.extraction"))
                    }
                }

                override fun visitJetElement(element: JetElement) {
                    if (element == body) {
                        validateBody()
                        return
                    }
                    super.visitJetElement(element)
                }
            }
    )

    return ExtractableCodeDescriptorWithConflicts(this, conflicts)
}

private val LOG = Logger.getInstance(javaClass<ExtractionEngine>())