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
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.core.refactoring.createTempCopy
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.ErrorMessage
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.Status
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.*
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.approximateWithResolvableType
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.isResolvableInScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.isSynthesizedInvoke
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.DFS.*
import java.util.*

internal val KotlinBuiltIns.defaultReturnType: KotlinType get() = unitType
internal val KotlinBuiltIns.defaultParameterType: KotlinType get() = nullableAnyType

private fun DeclarationDescriptor.renderForMessage(): String =
        IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(this)

private val TYPE_RENDERER = DescriptorRenderer.FQ_NAMES_IN_TYPES.withOptions {
    typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
}

private fun KotlinType.renderForMessage(): String = TYPE_RENDERER.renderType(this)

private fun KtDeclaration.renderForMessage(bindingContext: BindingContext): String? =
    bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this]?.renderForMessage()

internal fun KotlinType.isDefault(): Boolean = KotlinBuiltIns.isUnit(this)

private fun List<Instruction>.getModifiedVarDescriptors(bindingContext: BindingContext): Map<VariableDescriptor, List<KtExpression>> {
    val result = HashMap<VariableDescriptor, MutableList<KtExpression>>()
    for (instruction in filterIsInstance<WriteValueInstruction>()) {
        val expression = instruction.element as? KtExpression
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
                    doTraversal(it.body.enterInstruction)
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
        targetScope: LexicalScope?,
        options: ExtractionOptions,
        module: ModuleDescriptor
): Pair<KotlinType, List<KtExpression>> {
    fun instructionToExpression(instruction: Instruction, unwrapReturn: Boolean): KtExpression? {
        return when (instruction) {
            is ReturnValueInstruction ->
                (if (unwrapReturn) null else instruction.returnExpressionIfAny) ?: instruction.returnedValue.element as? KtExpression
            is InstructionWithValue ->
                instruction.outputValue?.element as? KtExpression
            else -> null
        }
    }

    fun instructionToType(instruction: Instruction): KotlinType? {
        val expression = instructionToExpression(instruction, true) ?: return null

        if (options.inferUnitTypeForUnusedValues && expression.isUsedAsStatement(bindingContext)) return null

        return bindingContext.getType(expression)
               ?: (expression as? KtReferenceExpression)?.let {
                   (bindingContext[BindingContext.REFERENCE_TARGET, it] as? CallableDescriptor)?.returnType
               }
    }

    val resultTypes = map(::instructionToType).filterNotNull()
    var commonSupertype = if (resultTypes.isNotEmpty()) CommonSupertypes.commonSupertype(resultTypes) else module.builtIns.defaultReturnType
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
    return singleSuccessorCheckingVisitor.target ?: instructions.firstOrNull()?.owner?.sinkInstruction
}

private fun KotlinType.isMeaningful(): Boolean {
    return !KotlinBuiltIns.isUnit(this) && !KotlinBuiltIns.isNothing(this)
}

private fun ExtractionData.getLocalDeclarationsWithNonLocalUsages(
        pseudocode: Pseudocode,
        localInstructions: List<Instruction>,
        bindingContext: BindingContext
): List<KtNamedDeclaration> {
    val declarations = HashSet<KtNamedDeclaration>()
    pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
        if (instruction !in localInstructions) {
            instruction.getPrimaryDeclarationDescriptorIfAny(bindingContext)?.let { descriptor ->
                val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
                if (declaration is KtNamedDeclaration && declaration.isInsideOf(originalElements)) {
                    declarations.add(declaration)
                }
            }
        }
    }
    return declarations.sortedBy { it.textRange!!.startOffset }
}

private fun ExtractionData.analyzeControlFlow(
        localInstructions: List<Instruction>,
        pseudocode: Pseudocode,
        module: ModuleDescriptor,
        bindingContext: BindingContext,
        modifiedVarDescriptors: Map<VariableDescriptor, List<KtExpression>>,
        options: ExtractionOptions,
        targetScope: LexicalScope?,
        parameters: Set<Parameter>
): Pair<ControlFlow, ErrorMessage?> {
    val exitPoints = localInstructions.getExitPoints()

    val valuedReturnExits = ArrayList<ReturnValueInstruction>()
    val defaultExits = ArrayList<Instruction>()
    val jumpExits = ArrayList<AbstractJumpInstruction>()
    exitPoints.forEach {
        val e = (it as? UnconditionalJumpInstruction)?.element
        val inst =
                when {
                    it !is ReturnValueInstruction && it !is ReturnNoValueInstruction && it.owner != pseudocode ->
                        null
                    e != null && e !is KtBreakExpression && e !is KtContinueExpression ->
                        it.previousInstructions.firstOrNull()
                    else ->
                        it
                }

        when (inst) {
            is ReturnValueInstruction -> {
                if (inst.owner == pseudocode) {
                    if (inst.returnExpressionIfAny == null) {
                        defaultExits.add(inst)
                    }
                    else {
                        valuedReturnExits.add(inst)
                    }
                }
            }

            is AbstractJumpInstruction -> {
                val element = inst.element
                if ((element is KtReturnExpression && inst.owner == pseudocode)
                        || element is KtBreakExpression
                        || element is KtContinueExpression) {
                    jumpExits.add(inst)
                }
                else if (element !is KtThrowExpression) {
                    defaultExits.add(inst)
                }
            }

            else -> if (inst != null && inst !is LocalFunctionDeclarationInstruction) {
                defaultExits.add(inst)
            }
        }
    }

    val nonLocallyUsedDeclarations = getLocalDeclarationsWithNonLocalUsages(pseudocode, localInstructions, bindingContext)
    val (declarationsToCopy, declarationsToReport) = nonLocallyUsedDeclarations.partition { it is KtProperty && it.isLocal }

    val (typeOfDefaultFlow, defaultResultExpressions) = defaultExits.getResultTypeAndExpressions(bindingContext, targetScope, options, module)
    val (returnValueType, valuedReturnExpressions) = valuedReturnExits.getResultTypeAndExpressions(bindingContext, targetScope, options, module)

    val emptyControlFlow =
            ControlFlow(Collections.emptyList(), { OutputValueBoxer.AsTuple(it, module) }, declarationsToCopy)

    val defaultReturnType = if (returnValueType.isMeaningful()) returnValueType else typeOfDefaultFlow
    if (defaultReturnType.isError) return emptyControlFlow to ErrorMessage.ERROR_TYPES

    val controlFlow = if (defaultReturnType.isMeaningful()) {
        emptyControlFlow.copy(outputValues = Collections.singletonList(ExpressionValue(false, defaultResultExpressions, defaultReturnType)))
    }
    else {
        emptyControlFlow
    }

    if (declarationsToReport.isNotEmpty()) {
        val localVarStr = declarationsToReport.map { it.renderForMessage(bindingContext)!! }.distinct().sorted()
        return controlFlow to ErrorMessage.DECLARATIONS_ARE_USED_OUTSIDE.addAdditionalInfo(localVarStr)
    }

    val outParameters =
            parameters.filter { it.mirrorVarName != null && modifiedVarDescriptors.getRaw(it.originalDescriptor) != null }.sortedBy { it.nameForRef }
    val outDeclarations =
            declarationsToCopy.filter { modifiedVarDescriptors.getRaw(bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it]) != null }
    val modifiedValueCount = outParameters.size + outDeclarations.size

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
            if (valuedReturnExits.size != 1) return multipleExitsError

            val element = valuedReturnExits.first().element as KtExpression
            return controlFlow.copy(outputValues = Collections.singletonList(Jump(listOf(element), element, true, module.builtIns))) to null
        }

        if (getCommonNonTrivialSuccessorIfAny(valuedReturnExits) == null) return multipleExitsError
        outputValues.add(ExpressionValue(true, valuedReturnExpressions, returnValueType))
    }

    outDeclarations.mapTo(outputValues) {
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it] as? CallableDescriptor
        Initializer(it as KtProperty, descriptor?.returnType ?: module.builtIns.defaultParameterType)
    }
    outParameters.mapTo(outputValues) { ParameterUpdate(it, modifiedVarDescriptors.getRaw(it.originalDescriptor)!!) }

    if (outputValues.isNotEmpty()) {
        if (jumpExits.isNotEmpty()) return outputAndExitsError

        val boxerFactory: (List<OutputValue>) -> OutputValueBoxer = when {
            outputValues.size > 3 -> {
                if (!options.enableListBoxing) {
                    val outValuesStr =
                            (outParameters.map { it.originalDescriptor.renderForMessage() }
                             + outDeclarations.map { it.renderForMessage(bindingContext)!! }).sorted()
                    return controlFlow to ErrorMessage.MULTIPLE_OUTPUT.addAdditionalInfo(outValuesStr)
                }
                OutputValueBoxer::AsList
            }

            else -> controlFlow.boxerFactory
        }

        return controlFlow.copy(outputValues = outputValues, boxerFactory = boxerFactory) to null
    }

    if (jumpExits.isNotEmpty()) {
        val jumpTarget = getCommonNonTrivialSuccessorIfAny(jumpExits) ?: return multipleExitsError

        val singleExit = getCommonNonTrivialSuccessorIfAny(defaultExits) == jumpTarget
        val conditional = !singleExit && defaultExits.isNotEmpty()
        val elements = jumpExits.map { it.element as KtExpression }
        val elementToInsertAfterCall = if (singleExit) null else elements.first()
        return controlFlow.copy(outputValues = Collections.singletonList(Jump(elements, elementToInsertAfterCall, conditional, module.builtIns))) to null
    }

    return controlFlow to null
}

fun ExtractionData.createTemporaryDeclaration(functionText: String): KtNamedDeclaration {
    val textRange = targetSibling.textRange!!

    val insertText: String
    val insertPosition: Int
    val lookupPosition: Int
    if (insertBefore) {
        insertPosition = textRange.startOffset
        lookupPosition = insertPosition
        insertText = functionText
    }
    else {
        insertPosition = textRange.endOffset
        lookupPosition = insertPosition + 1
        insertText = "\n$functionText"
    }

    val tmpFile = originalFile.createTempCopy { text ->
        StringBuilder(text).insert(insertPosition, insertText).toString()
    }
    return tmpFile.findElementAt(lookupPosition)?.getNonStrictParentOfType<KtNamedDeclaration>()!!
}

private fun ExtractionData.createTemporaryCodeBlock(): KtBlockExpression =
        (createTemporaryDeclaration("fun() {\n$codeFragmentText\n}\n") as KtNamedFunction).bodyExpression as KtBlockExpression

private fun KotlinType.collectReferencedTypes(processTypeArguments: Boolean): List<KotlinType> {
    if (!processTypeArguments) return Collections.singletonList(this)
    return DFS.dfsFromNode(
            this,
            object: Neighbors<KotlinType> {
                override fun getNeighbors(current: KotlinType): Iterable<KotlinType> = current.arguments.map { it.type }
            },
            VisitedWithSet(),
            object: CollectingNodeHandler<KotlinType, KotlinType, ArrayList<KotlinType>>(ArrayList()) {
                override fun afterChildren(current: KotlinType) {
                    result.add(current)
                }
            }
    )!!
}

fun KtTypeParameter.collectRelevantConstraints(): List<KtTypeConstraint> {
    val typeConstraints = getNonStrictParentOfType<KtTypeParameterListOwner>()?.typeConstraints ?: return Collections.emptyList()
    return typeConstraints.filter { it.subjectTypeParameterName?.mainReference?.resolve() == this}
}

fun TypeParameter.collectReferencedTypes(bindingContext: BindingContext): List<KotlinType> {
    val typeRefs = ArrayList<KtTypeReference>()
    originalDeclaration.extendsBound?.let { typeRefs.add(it) }
    originalConstraints
            .map { it.boundTypeReference }
            .filterNotNullTo(typeRefs)

    return typeRefs
            .map { bindingContext[BindingContext.TYPE, it] }
            .filterNotNull()
}

private fun KotlinType.isExtractable(targetScope: LexicalScope?): Boolean {
    return collectReferencedTypes(true).fold(true) { extractable, typeToCheck ->
        val parameterTypeDescriptor = typeToCheck.constructor.declarationDescriptor as? TypeParameterDescriptor
        val typeParameter = parameterTypeDescriptor?.let {
            DescriptorToSourceUtils.descriptorToDeclaration(it)
        } as? KtTypeParameter

        extractable && (typeParameter != null || typeToCheck.isResolvableInScope(targetScope, false))
    }
}

private fun KotlinType.processTypeIfExtractable(
        typeParameters: MutableSet<TypeParameter>,
        nonDenotableTypes: MutableSet<KotlinType>,
        options: ExtractionOptions,
        targetScope: LexicalScope?,
        processTypeArguments: Boolean = true
): Boolean {
    return collectReferencedTypes(processTypeArguments).fold(true) { extractable, typeToCheck ->
        val parameterTypeDescriptor = typeToCheck.constructor.declarationDescriptor as? TypeParameterDescriptor
        val typeParameter = parameterTypeDescriptor?.let {
            DescriptorToSourceUtils.descriptorToDeclaration(it)
        } as? KtTypeParameter

        when {
            typeToCheck.isResolvableInScope(targetScope, true) ->
                extractable

            typeParameter != null -> {
                typeParameters.add(TypeParameter(typeParameter, typeParameter.collectRelevantConstraints()))
                extractable
            }

            options.allowSpecialClassNames && typeToCheck.isSpecial() ->
                extractable

            typeToCheck.isError ->
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
        private val targetScope: LexicalScope?,
        private val originalType: KotlinType,
        private val possibleTypes: Set<KotlinType>
): Parameter {
    // All modifications happen in the same thread
    private var writable: Boolean = true
    private val defaultTypes = LinkedHashSet<KotlinType>()
    private val typePredicates = HashSet<TypePredicate>()

    var refCount: Int = 0

    fun addDefaultType(jetType: KotlinType) {
        assert(writable) { "Can't add type to non-writable parameter $currentName" }

        if (jetType in possibleTypes) {
            defaultTypes.add(jetType)
        }
    }

    fun addTypePredicate(predicate: TypePredicate) {
        assert(writable) { "Can't add type predicate to non-writable parameter $currentName" }
        typePredicates.add(predicate)
    }

    var currentName: String? = null
    override val name: String get() = currentName!!

    override var mirrorVarName: String? = null

    private val defaultType: KotlinType by lazy {
        writable = false
        if (defaultTypes.isNotEmpty()) {
            TypeIntersector.intersectTypes(KotlinTypeChecker.DEFAULT, defaultTypes)!!
        }
        else originalType
    }

    private val parameterTypeCandidates: List<KotlinType> by lazy {
        writable = false

        val typePredicate = and(typePredicates)

        val typeSet = if (defaultType.isFlexible()) {
            val bounds = defaultType.getCapability(Flexibility::class.java)!!
            LinkedHashSet<KotlinType>().apply {
                if (typePredicate(bounds.upperBound)) add(bounds.upperBound)
                if (typePredicate(bounds.lowerBound)) add(bounds.lowerBound)
            }
        }
        else linkedSetOf(defaultType)

        val addNullableTypes = defaultType.isNullabilityFlexible() && typeSet.size > 1
        val superTypes = TypeUtils.getAllSupertypes(defaultType).filter(typePredicate)

        for (superType in superTypes) {
            if (addNullableTypes) {
                typeSet.add(superType.makeNullable())
            }
            typeSet.add(superType)
        }

        typeSet.toList()
    }

    override fun getParameterTypeCandidates(allowSpecialClassNames: Boolean): List<KotlinType> {
            return if (!allowSpecialClassNames) {
                parameterTypeCandidates.filter { it.isExtractable(targetScope) }
            } else {
                parameterTypeCandidates
            }
    }

    override fun getParameterType(allowSpecialClassNames: Boolean): KotlinType {
        return getParameterTypeCandidates(allowSpecialClassNames).firstOrNull() ?: defaultType
    }

    override fun copy(name: String, parameterType: KotlinType): Parameter = DelegatingParameter(this, name, parameterType)
}

private class DelegatingParameter(
        val original: Parameter,
        override val name: String,
        val parameterType: KotlinType
): Parameter by original {
    override fun copy(name: String, parameterType: KotlinType): Parameter = DelegatingParameter(original, name, parameterType)
    override fun getParameterType(allowSpecialClassNames: Boolean) = parameterType
}

private class ParametersInfo {
    var errorMessage: ErrorMessage? = null
    val replacementMap: MutableMap<Int, Replacement> = HashMap()
    val originalRefToParameter: MutableMap<KtSimpleNameExpression, MutableParameter> = HashMap()
    val parameters: MutableSet<MutableParameter> = HashSet()
    val typeParameters: MutableSet<TypeParameter> = HashSet()
    val nonDenotableTypes: MutableSet<KotlinType> = HashSet()
}

private fun ExtractionData.inferParametersInfo(
        commonParent: PsiElement,
        pseudocode: Pseudocode,
        bindingContext: BindingContext,
        targetScope: LexicalScope,
        modifiedVarDescriptors: Set<VariableDescriptor>
): ParametersInfo {
    val info = ParametersInfo()

    val extractedDescriptorToParameter = HashMap<DeclarationDescriptor, MutableParameter>()

    fun suggestParameterType(
            extractFunctionRef: Boolean,
            originalDescriptor: DeclarationDescriptor,
            parameterExpression: KtExpression?,
            receiverToExtract: ReceiverValue,
            resolvedCall: ResolvedCall<*>?,
            useSmartCastsIfPossible: Boolean
    ): KotlinType {
        val builtIns = originalDescriptor.builtIns
        return when {
                   extractFunctionRef -> {
                       originalDescriptor as FunctionDescriptor
                       builtIns.getFunctionType(Annotations.EMPTY,
                                                originalDescriptor.extensionReceiverParameter?.type,
                                                originalDescriptor.valueParameters.map { it.type },
                                                originalDescriptor.returnType ?: builtIns.defaultReturnType)
                   }
                   parameterExpression != null ->
                       (if (useSmartCastsIfPossible) bindingContext[BindingContext.SMARTCAST, parameterExpression] else null)
                       ?: bindingContext.getType(parameterExpression)
                       ?: (parameterExpression as? KtReferenceExpression)?.let {
                           (bindingContext[BindingContext.REFERENCE_TARGET, it] as? CallableDescriptor)?.returnType
                       }
                       ?: if (receiverToExtract.exists()) receiverToExtract.type else null
                   receiverToExtract is ThisReceiver -> {
                       val calleeExpression = resolvedCall!!.call.calleeExpression
                       val typeByDataFlowInfo = if (useSmartCastsIfPossible) {
                           bindingContext[BindingContext.EXPRESSION_TYPE_INFO, calleeExpression]?.dataFlowInfo?.let { dataFlowInfo ->
                               val possibleTypes = dataFlowInfo.getPossibleTypes(DataFlowValueFactory.createDataFlowValue(receiverToExtract))
                               if (possibleTypes.isNotEmpty()) CommonSupertypes.commonSupertype(possibleTypes) else null
                           }
                       } else null
                       typeByDataFlowInfo ?: receiverToExtract.type
                   }
                   receiverToExtract.exists() -> receiverToExtract.type
                   else -> null
               } ?: builtIns.defaultParameterType
    }

    for (refInfo in getBrokenReferencesInfo(createTemporaryCodeBlock())) {
        val (originalRef, originalDeclaration, originalDescriptor, resolvedCall) = refInfo.resolveResult
        val ref = refInfo.refExpr

        val selector = (ref.parent as? KtCallExpression) ?: ref
        val superExpr = (selector.parent as? KtQualifiedExpression)?.receiverExpression as? KtSuperExpression
        if (superExpr != null) {
            info.errorMessage = ErrorMessage.SUPER_CALL
            return info
        }

        val extensionReceiver = resolvedCall?.extensionReceiver
        val receiverToExtract = when {
                           extensionReceiver == ReceiverValue.NO_RECEIVER,
                           isSynthesizedInvoke(originalDescriptor) -> resolvedCall?.dispatchReceiver
                           else -> extensionReceiver
                       } ?: ReceiverValue.NO_RECEIVER

        val thisDescriptor = (receiverToExtract as? ThisReceiver)?.declarationDescriptor
        val hasThisReceiver = thisDescriptor != null
        val thisExpr = ref.parent as? KtThisExpression

        if (hasThisReceiver
            && DescriptorToSourceUtilsIde.getAllDeclarations(project, thisDescriptor!!).all { it.isInsideOf(originalElements) }) {
            continue
        }

        val referencedClassifierDescriptor: ClassifierDescriptor? = (thisDescriptor ?: originalDescriptor).let {
            when (it) {
                is ClassDescriptor ->
                    when(it.kind) {
                        ClassKind.OBJECT, ClassKind.ENUM_CLASS -> it
                        ClassKind.ENUM_ENTRY -> it.containingDeclaration as? ClassDescriptor
                        else -> if (ref.getNonStrictParentOfType<KtTypeReference>() != null) it else null
                    }

                is TypeParameterDescriptor -> it

                is ConstructorDescriptor -> it.containingDeclaration

                else -> null
            } as? ClassifierDescriptor
        }

        if (referencedClassifierDescriptor != null) {
            if (!referencedClassifierDescriptor.defaultType.processTypeIfExtractable(
                    info.typeParameters, info.nonDenotableTypes, options, targetScope, referencedClassifierDescriptor is TypeParameterDescriptor
            )) continue

            if (referencedClassifierDescriptor is ClassDescriptor) {
                info.replacementMap[refInfo.offsetInBody] = FqNameReplacement(originalDescriptor.getImportableDescriptor().fqNameSafe)
            }
        }
        else {
            val extractThis = (hasThisReceiver && refInfo.smartCast == null) || thisExpr != null
            val extractOrdinaryParameter =
                    originalDeclaration is KtMultiDeclarationEntry ||
                            originalDeclaration is KtProperty ||
                            originalDeclaration is KtParameter

            val extractFunctionRef =
                    options.captureLocalFunctions
                    && originalRef.getReferencedName() == originalDescriptor.name.asString() // to forbid calls by convention
                    && originalDeclaration is KtNamedFunction && originalDeclaration.isLocal
                    && targetScope.findFunction(originalDescriptor.name, NoLookupLocation.FROM_IDE) { it == originalDescriptor } == null

            val descriptorToExtract = (if (extractThis) thisDescriptor else null) ?: originalDescriptor

            val extractParameter = extractThis || extractOrdinaryParameter || extractFunctionRef
            if (extractParameter) {
                val parameterExpression = when {
                    receiverToExtract is ExpressionReceiver -> {
                        val receiverExpression = receiverToExtract.expression
                        // If p.q has a smart-cast, then extract entire qualified expression
                        if (refInfo.smartCast != null) receiverExpression.parent as KtExpression else receiverExpression
                    }
                    receiverToExtract.exists() && refInfo.smartCast == null -> null
                    else -> (originalRef.parent as? KtThisExpression) ?: originalRef
                }

                val parameterType = suggestParameterType(extractFunctionRef, originalDescriptor, parameterExpression, receiverToExtract, resolvedCall, true)

                val parameter = extractedDescriptorToParameter.getOrPut(descriptorToExtract) {
                    var argumentText =
                            if (hasThisReceiver && extractThis) {
                                val label = if (descriptorToExtract is ClassDescriptor) "@${descriptorToExtract.name.asString()}" else ""
                                "this$label"
                            }
                            else {
                                val argumentExpr = (thisExpr ?: ref).getQualifiedExpressionForSelectorOrThis()
                                if (argumentExpr is KtOperationReferenceExpression) {
                                    val nameElement = argumentExpr.getReferencedNameElement()
                                    val nameElementType = nameElement.node.elementType
                                    (nameElementType as? KtToken)?.let {
                                        OperatorConventions.getNameForOperationSymbol(it)?.asString()
                                    } ?: nameElement.text
                                }
                                else argumentExpr.text
                                     ?: throw AssertionError("reference shouldn't be empty: code fragment = $codeFragmentText")
                            }
                    if (extractFunctionRef) {
                        val receiverTypeText = (originalDeclaration as KtCallableDeclaration).receiverTypeReference?.text ?: ""
                        argumentText = "$receiverTypeText::$argumentText"
                    }

                    val originalType = suggestParameterType(extractFunctionRef, originalDescriptor, parameterExpression, receiverToExtract, resolvedCall, false)

                    MutableParameter(argumentText, descriptorToExtract, extractThis, targetScope, originalType, refInfo.possibleTypes)
                }

                if (!extractThis) {
                    parameter.currentName = originalDeclaration.nameIdentifier?.text
                }

                parameter.refCount++
                info.originalRefToParameter[originalRef] = parameter

                parameter.addDefaultType(parameterType)

                if (extractThis && thisExpr == null) {
                    val callElement = resolvedCall!!.call.callElement
                    val instruction = pseudocode.getElementValue(callElement)?.createdAt as? InstructionWithReceivers
                    val receiverValue = instruction?.receiverValues?.entries?.singleOrNull { it.value == receiverToExtract }?.key
                    if (receiverValue != null) {
                        parameter.addTypePredicate(getExpectedTypePredicate(receiverValue, bindingContext, targetScope.ownerDescriptor.builtIns))
                    }
                }
                else if (extractFunctionRef) {
                    parameter.addTypePredicate(SingleType(parameterType))
                }
                else {
                    pseudocode.getElementValuesRecursively(originalRef).forEach {
                        parameter.addTypePredicate(getExpectedTypePredicate(it, bindingContext, targetScope.ownerDescriptor.builtIns))
                    }
                }

                info.replacementMap[refInfo.offsetInBody] =
                        if (hasThisReceiver && extractThis) AddPrefixReplacement(parameter) else RenameReplacement(parameter)
            }
        }
    }

    val varNameValidator = NewDeclarationNameValidator(
            commonParent.getNonStrictParentOfType<KtExpression>()!!,
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
            mirrorVarName = if (modifiedVarDescriptors.containsRaw(descriptorToExtract)) KotlinNameSuggester.suggestNameByName(name, varNameValidator) else null
            info.parameters.add(this)
        }
    }

    for (typeToCheck in info.typeParameters.flatMapTo(HashSet<KotlinType>()) { it.collectReferencedTypes(bindingContext) }) {
        typeToCheck.processTypeIfExtractable(info.typeParameters, info.nonDenotableTypes, options, targetScope)
    }


    return info
}

private fun ExtractionData.checkDeclarationsMovingOutOfScope(
        enclosingDeclaration: KtDeclaration,
        controlFlow: ControlFlow,
        bindingContext: BindingContext
): ErrorMessage? {
    val declarationsOutOfScope = HashSet<KtNamedDeclaration>()
    controlFlow.jumpOutputValue?.elementToInsertAfterCall?.accept(
            object : KtTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    val target = expression.mainReference.resolve()
                    if (target is KtNamedDeclaration
                        && target.isInsideOf(originalElements)
                        && target.getStrictParentOfType<KtDeclaration>() == enclosingDeclaration) {
                        declarationsOutOfScope.add(target)
                    }
                }
            }
    )

    if (declarationsOutOfScope.isNotEmpty()) {
        val declStr = declarationsOutOfScope.map { it.renderForMessage(bindingContext)!! }.sorted()
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
    return when (targetSibling.parent) {
        is KtClassBody, is KtFile -> true
        else -> false
    }
}

fun ExtractionData.getDefaultVisibility(): String {
    if (!isVisibilityApplicable()) return ""

    val parent = targetSibling.getStrictParentOfType<KtDeclaration>()
    if (parent is KtClass) {
        if (parent.isInterface()) return ""
        if (parent.isEnum() && commonParent.getNonStrictParentOfType<KtEnumEntry>()?.getStrictParentOfType<KtClass>() == parent) return ""
    }

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
    val paramsInfo = inferParametersInfo(commonParent, pseudocode, bindingContext, targetScope, modifiedVarDescriptorsWithExpressions.keys)
    if (paramsInfo.errorMessage != null) {
        return AnalysisResult(null, Status.CRITICAL_ERROR, listOf(paramsInfo.errorMessage!!))
    }

    val messages = ArrayList<ErrorMessage>()

    val modifiedVarDescriptorsForControlFlow = HashMap(modifiedVarDescriptorsWithExpressions)
    modifiedVarDescriptorsForControlFlow.keys.retainAll(localInstructions.getVarDescriptorsAccessedAfterwards(bindingContext))
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
        val typeStr = paramsInfo.nonDenotableTypes.map {it.renderForMessage()}.sorted()
        return AnalysisResult(
                null,
                Status.CRITICAL_ERROR,
                listOf(ErrorMessage.DENOTABLE_TYPES.addAdditionalInfo(typeStr))
        )
    }

    val enclosingDeclaration = commonParent.getStrictParentOfType<KtDeclaration>()!!
    checkDeclarationsMovingOutOfScope(enclosingDeclaration, controlFlow, bindingContext)?.let { messages.add(it) }

    controlFlow.jumpOutputValue?.elementToInsertAfterCall?.accept(
            object : KtTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    paramsInfo.originalRefToParameter[expression]?.let { it.refCount-- }
                }
            }
    )
    val adjustedParameters = paramsInfo.parameters.filterTo(HashSet<Parameter>()) { it.refCount > 0 }

    val receiverCandidates = adjustedParameters.filterTo(HashSet<Parameter>()) { it.receiverCandidate }
    val receiverParameter = if (receiverCandidates.size == 1) receiverCandidates.first() else null
    receiverParameter?.let { adjustedParameters.remove(it) }

    return AnalysisResult(
            ExtractableCodeDescriptor(
                    this,
                    bindingContext,
                    suggestFunctionNames(returnType),
                    getDefaultVisibility(),
                    adjustedParameters.sortedBy { it.name },
                    receiverParameter,
                    paramsInfo.typeParameters.sortedBy { it.originalDeclaration.name!! },
                    paramsInfo.replacementMap,
                    if (messages.isEmpty()) controlFlow else controlFlow.toDefault(),
                    returnType
            ),
            if (messages.isEmpty()) Status.SUCCESS else Status.NON_CRITICAL_ERROR,
            messages
    )
}

private fun ExtractionData.suggestFunctionNames(returnType: KotlinType): List<String> {
    val functionNames = LinkedHashSet<String>()

    val validator =
            NewDeclarationNameValidator(
                    targetSibling.parent,
                    if (targetSibling is KtClassInitializer) targetSibling.parent else targetSibling,
                    if (options.extractAsProperty) NewDeclarationNameValidator.Target.VARIABLES else NewDeclarationNameValidator.Target.FUNCTIONS_AND_CLASSES
            )
    if (!returnType.isDefault()) {
        functionNames.addAll(KotlinNameSuggester.suggestNamesByType(returnType, validator))
    }

    getExpressions().singleOrNull()?.let { expr ->
        val property = expr.getStrictParentOfType<KtProperty>()
        if (property?.initializer == expr) {
            property?.name?.let { functionNames.add(KotlinNameSuggester.suggestNameByName("get" + it.capitalize(), validator)) }
        }
    }

    return functionNames.toList()
}

internal fun KtNamedDeclaration.getGeneratedBody() =
        when (this) {
            is KtNamedFunction -> bodyExpression
            else -> {
                val property = this as KtProperty

                property.getter?.bodyExpression?.let { return it }
                property.initializer?.let { return it }
                // We assume lazy property here with delegate expression 'by Delegates.lazy { body }'
                property.delegateExpression?.let {
                    val call = it.getCalleeExpressionIfAny()?.parent as? KtCallExpression
                    call?.functionLiteralArguments?.singleOrNull()?.getFunctionLiteral()?.bodyExpression
                }
            }
        } ?: throw AssertionError("Couldn't get block body for this declaration: ${getElementTextWithContext()}")

fun ExtractableCodeDescriptor.validate(): ExtractableCodeDescriptorWithConflicts {
    fun getDeclarationMessage(declaration: PsiNamedElement, messageKey: String, capitalize: Boolean = true): String {
        val message = KotlinRefactoringBundle.message(messageKey, RefactoringUIUtil.getDescription(declaration, true))
        return if (capitalize) message.capitalize() else message
    }

    val conflicts = MultiMap<PsiElement, String>()

    val result = ExtractionGeneratorConfiguration(
            this,
            ExtractionGeneratorOptions(inTempFile = true, allowExpressionBody = false)
    ).generateDeclaration()

    val valueParameterList = (result.declaration as? KtNamedFunction)?.valueParameterList
    val typeParameterList = (result.declaration as? KtNamedFunction)?.typeParameterList
    val body = result.declaration.getGeneratedBody()
    val bindingContext = body.analyzeFully()

    fun validateBody() {
        for ((originalOffset, resolveResult) in extractionData.refOffsetToDeclaration) {
            if (resolveResult.declaration.isInsideOf(extractionData.originalElements)) continue

            val currentRefExpr = result.nameByOffset[originalOffset]?.let {
                (it as? KtThisExpression)?.instanceReference ?: it as? KtSimpleNameExpression
            } ?: continue

            if (currentRefExpr.parent is KtThisExpression) continue

            val diagnostics = bindingContext.diagnostics.forElement(currentRefExpr)

            val currentDescriptor = bindingContext[BindingContext.REFERENCE_TARGET, currentRefExpr]
            val currentTarget =
                    currentDescriptor?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(extractionData.project, it) } as? PsiNamedElement
            if (currentTarget is KtParameter && currentTarget.parent == valueParameterList) continue
            if (currentTarget is KtTypeParameter && currentTarget.parent == typeParameterList) continue
            if (currentDescriptor is LocalVariableDescriptor
                && parameters.any { it.mirrorVarName == currentDescriptor.name.asString() }) continue

            if (diagnostics.any { it.factory in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS }
                || (currentDescriptor != null
                    && !ErrorUtils.isError(currentDescriptor)
                    && !compareDescriptors(extractionData.project, currentDescriptor, resolveResult.descriptor))) {
                conflicts.putValue(
                        resolveResult.originalRefExpr,
                        getDeclarationMessage(resolveResult.declaration, "0.will.no.longer.be.accessible.after.extraction")
                )
                continue
            }

            diagnostics.firstOrNull { it.factory in Errors.INVISIBLE_REFERENCE_DIAGNOSTICS }?.let {
                val message = when (it.factory) {
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
            object : KtTreeVisitorVoid() {
                override fun visitUserType(userType: KtUserType) {
                    val refExpr = userType.referenceExpression ?: return
                    val declaration = refExpr.mainReference.resolve() as? PsiNamedElement ?: return
                    val diagnostics = bindingContext.diagnostics.forElement(refExpr)
                    diagnostics.firstOrNull { it.factory == Errors.INVISIBLE_REFERENCE }?.let {
                        conflicts.putValue(declaration, getDeclarationMessage(declaration, "0.will.become.invisible.after.extraction"))
                    }
                }

                override fun visitKtElement(element: KtElement) {
                    if (element == body) {
                        validateBody()
                        return
                    }
                    super.visitKtElement(element)
                }
            }
    )

    return ExtractableCodeDescriptorWithConflicts(this, conflicts)
}

private val LOG = Logger.getInstance(ExtractionEngine::class.java)
