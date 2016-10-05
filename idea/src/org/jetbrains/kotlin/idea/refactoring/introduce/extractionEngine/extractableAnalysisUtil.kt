/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.cfg.pseudocode.instructions.KtElementInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.MarkInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraverseInstructionResult
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverseFollowingInstructions
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.createTempCopy
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.ErrorMessage
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.Status
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.*
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.approximateWithResolvableType
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.isResolvableInScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
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
        val descriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, bindingContext)
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
        traverseFollowingInstructions(instruction, visitedInstructions) {
            when {
                it is AccessValueInstruction && it !in this ->
                    PseudocodeUtil.extractVariableDescriptorIfAny(it, bindingContext)?.let { accessedAfterwards.add(it) }

                it is LocalFunctionDeclarationInstruction ->
                    doTraversal(it.body.enterInstruction)
            }

            TraverseInstructionResult.CONTINUE
        }
    }

    forEach(::doTraversal)
    return accessedAfterwards
}

private fun List<Instruction>.getExitPoints(): List<Instruction> =
        filter { localInstruction -> localInstruction.nextInstructions.any { it !in this } }

private fun ExtractionData.getResultTypeAndExpressions(
        instructions: List<Instruction>,
        bindingContext: BindingContext,
        targetScope: LexicalScope?,
        options: ExtractionOptions, module: ModuleDescriptor
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

        substringInfo?.let {
            if (it.template == expression) return it.type
        }

        if (options.inferUnitTypeForUnusedValues && expression.isUsedAsStatement(bindingContext)) return null

        return bindingContext.getType(expression)
               ?: (expression as? KtReferenceExpression)?.let {
                   (bindingContext[BindingContext.REFERENCE_TARGET, it] as? CallableDescriptor)?.returnType
               }
    }

    val resultTypes = instructions.mapNotNull(::instructionToType)
    val commonSupertype = if (resultTypes.isNotEmpty()) CommonSupertypes.commonSupertype(resultTypes) else module.builtIns.defaultReturnType
    val resultType = if (options.allowSpecialClassNames) commonSupertype else commonSupertype.approximateWithResolvableType(targetScope, false)

    val expressions = instructions.mapNotNull { instructionToExpression(it, false) }

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
                if (declaration is KtNamedDeclaration && declaration.isInsideOf(physicalElements)) {
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
                    it is UnconditionalJumpInstruction && it.targetLabel.isJumpToError ->
                        it
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
                else if (element !is KtThrowExpression && !inst.targetLabel.isJumpToError) {
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

    val (typeOfDefaultFlow, defaultResultExpressions) = getResultTypeAndExpressions(defaultExits, bindingContext, targetScope, options, module)
    val (returnValueType, valuedReturnExpressions) = getResultTypeAndExpressions(valuedReturnExits, bindingContext, targetScope, options, module)

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
            parameters.filter { it.mirrorVarName != null && modifiedVarDescriptors[it.originalDescriptor] != null }.sortedBy { it.nameForRef }
    val outDeclarations =
            declarationsToCopy.filter { modifiedVarDescriptors[bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it]] != null }
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
    outParameters.mapTo(outputValues) { ParameterUpdate(it, modifiedVarDescriptors[it.originalDescriptor]!!) }

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
                { outputValues -> OutputValueBoxer.AsList(outputValues) } // KT-8596
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

fun ExtractionData.createTemporaryDeclaration(pattern: String): KtNamedDeclaration {
    val targetSiblingMarker = Any()
    PsiTreeUtil.mark(targetSibling, targetSiblingMarker)
    val tmpFile = originalFile.createTempCopy("")
    tmpFile.deleteChildRange(tmpFile.firstChild, tmpFile.lastChild)
    tmpFile.addRange(originalFile.firstChild, originalFile.lastChild)
    val newTargetSibling = PsiTreeUtil.releaseMark(tmpFile, targetSiblingMarker)!!
    val newTargetParent = newTargetSibling.parent

    val declaration = KtPsiFactory(originalFile).createDeclarationByPattern<KtNamedDeclaration>(
            pattern,
            PsiChildRange(originalElements.firstOrNull(), originalElements.lastOrNull())
    )
    return if (insertBefore) {
        newTargetParent.addBefore(declaration, newTargetSibling) as KtNamedDeclaration
    }
    else {
        newTargetParent.addAfter(declaration, newTargetSibling) as KtNamedDeclaration
    }
}

internal fun ExtractionData.createTemporaryCodeBlock(): KtBlockExpression {
    if (options.extractAsProperty) {
        return ((createTemporaryDeclaration("val = {\n$0\n}\n") as KtProperty).initializer as KtLambdaExpression).bodyExpression!!
    }
    return (createTemporaryDeclaration("fun() {\n$0\n}\n") as KtNamedFunction).bodyExpression as KtBlockExpression
}

private fun KotlinType.collectReferencedTypes(processTypeArguments: Boolean): List<KotlinType> {
    if (!processTypeArguments) return Collections.singletonList(this)
    return DFS.dfsFromNode(
            this,
            Neighbors<KotlinType> { current -> current.arguments.map { it.type } },
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
    originalConstraints.mapNotNullTo(typeRefs) { it.boundTypeReference }

    return typeRefs.mapNotNull { bindingContext[BindingContext.TYPE, it] }
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

internal fun KotlinType.processTypeIfExtractable(
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

internal class MutableParameter(
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
            val bounds = defaultType.asFlexibleType()
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
                        && target.isInsideOf(physicalElements)
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
        if (it is KtElementInstruction && it.element.isInsideOf(physicalElements)) {
            instructions.add(it)
        }
    }
    return instructions
}

fun ExtractionData.isVisibilityApplicable(): Boolean {
    val parent = targetSibling.parent
    return parent is KtClassBody || (parent is KtFile && !parent.isScript)
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

    val declaration = commonParent.containingDeclarationForPseudocode ?: return noContainerError
    val pseudocode = declaration.getContainingPseudocode(bindingContext)
                     ?: return AnalysisResult(null, Status.CRITICAL_ERROR, listOf(ErrorMessage.SYNTAX_ERRORS))
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
                    paramsInfo.originalRefToParameter[expression].firstOrNull()?.let { it.refCount-- }
                }
            }
    )
    val adjustedParameters = paramsInfo.parameters.filterTo(HashSet<Parameter>()) { it.refCount > 0 }

    val receiverCandidates = adjustedParameters.filterTo(HashSet<Parameter>()) { it.receiverCandidate }
    val receiverParameter = if (receiverCandidates.size == 1 && !options.canWrapInWith) receiverCandidates.first() else null
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
                    if (targetSibling is KtAnonymousInitializer) targetSibling.parent else targetSibling,
                    if (options.extractAsProperty) NewDeclarationNameValidator.Target.VARIABLES else NewDeclarationNameValidator.Target.FUNCTIONS_AND_CLASSES
            )
    if (!returnType.isDefault()) {
        functionNames.addAll(KotlinNameSuggester.suggestNamesByType(returnType, validator))
    }

    expressions.singleOrNull()?.let { expr ->
        val property = expr.getStrictParentOfType<KtProperty>()
        if (property?.initializer == expr) {
            property.name?.let { functionNames.add(KotlinNameSuggester.suggestNameByName("get" + it.capitalize(), validator)) }
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
                    call?.lambdaArguments?.singleOrNull()?.getLambdaExpression()?.bodyExpression
                }
            }
        } ?: throw AssertionError("Couldn't get block body for this declaration: ${getElementTextWithContext()}")

@JvmOverloads
fun ExtractableCodeDescriptor.validate(target: ExtractionTarget = ExtractionTarget.FUNCTION): ExtractableCodeDescriptorWithConflicts {
    fun getDeclarationMessage(declaration: PsiNamedElement, messageKey: String, capitalize: Boolean = true): String {
        val message = KotlinRefactoringBundle.message(messageKey, RefactoringUIUtil.getDescription(declaration, true))
        return if (capitalize) message.capitalize() else message
    }

    val conflicts = MultiMap<PsiElement, String>()

    val result = ExtractionGeneratorConfiguration(
            this,
            ExtractionGeneratorOptions(inTempFile = true, allowExpressionBody = false, target = target)
    ).generateDeclaration()

    val valueParameterList = (result.declaration as? KtNamedFunction)?.valueParameterList
    val typeParameterList = (result.declaration as? KtNamedFunction)?.typeParameterList
    val body = result.declaration.getGeneratedBody()
    val bindingContext = body.analyzeFully()

    fun processReference(currentRefExpr: KtSimpleNameExpression) {
        val resolveResult = currentRefExpr.resolveResult ?: return
        if (currentRefExpr.parent is KtThisExpression) return

        val diagnostics = bindingContext.diagnostics.forElement(currentRefExpr)

        val currentDescriptor = bindingContext[BindingContext.REFERENCE_TARGET, currentRefExpr]
        val currentTarget =
                currentDescriptor?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(extractionData.project, it) } as? PsiNamedElement
        if (currentTarget is KtParameter && currentTarget.parent == valueParameterList) return
        if (currentTarget is KtTypeParameter && currentTarget.parent == typeParameterList) return
        if (currentDescriptor is LocalVariableDescriptor
            && parameters.any { it.mirrorVarName == currentDescriptor.name.asString() }) return

        if (diagnostics.any { it.factory in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS }
            || (currentDescriptor != null
                && !ErrorUtils.isError(currentDescriptor)
                && !compareDescriptors(extractionData.project, currentDescriptor, resolveResult.descriptor))) {
            conflicts.putValue(
                    resolveResult.originalRefExpr,
                    getDeclarationMessage(resolveResult.declaration, "0.will.no.longer.be.accessible.after.extraction")
            )
            return
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

                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    processReference(expression)
                }
            }
    )

    return ExtractableCodeDescriptorWithConflicts(this, conflicts)
}

private val LOG = Logger.getInstance(ExtractionEngine::class.java)
