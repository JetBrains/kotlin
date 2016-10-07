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

package org.jetbrains.kotlin.cfg

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg.TailRecursionKind.*
import org.jetbrains.kotlin.cfg.VariableUseState.*
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.kotlin.cfg.pseudocode.instructions.KtElementInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.MarkInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocode.sideEffectFree
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.Edges
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverseFollowingInstructions
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.referencedProperty
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsResultOfLambda
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getDispatchReceiverWithSmartCast
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.hasThisOrNoDispatchReceiver
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils.*
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import java.util.*

class ControlFlowInformationProvider private constructor(
        private val subroutine: KtElement,
        private val trace: BindingTrace,
        private val pseudocode: Pseudocode
) {

    private val pseudocodeVariablesData by lazy {
        PseudocodeVariablesData(pseudocode, trace.bindingContext)
    }

    constructor(declaration: KtElement, trace: BindingTrace)
    : this(declaration, trace, ControlFlowProcessor(trace).generatePseudocode(declaration)) {}

    fun checkForLocalClassOrObjectMode() {
        // Local classes and objects are analyzed twice: when TopDownAnalyzer processes it and as a part of its container.
        // Almost all checks can be done when the container is analyzed
        // except recording initialized variables (this information is needed for DeclarationChecker).
        recordInitializedVariables()
    }

    fun checkDeclaration() {

        recordInitializedVariables()

        checkLocalFunctions()

        markUninitializedVariables()

        if (trace.wantsDiagnostics()) {
            markUnusedVariables()
        }

        markStatements()

        markUnusedExpressions()

        if (trace.wantsDiagnostics()) {
            checkIfExpressions()
        }

        checkWhenExpressions()

        checkConstructorConsistency()
    }

    fun checkFunction(expectedReturnType: KotlinType?) {
        val unreachableCode = collectUnreachableCode()
        reportUnreachableCode(unreachableCode)

        if (subroutine is KtFunctionLiteral) return

        checkDefiniteReturn(expectedReturnType ?: NO_EXPECTED_TYPE, unreachableCode)

        markTailCalls()
    }

    private fun collectReturnExpressions(returnedExpressions: MutableCollection<KtElement>) {
        val instructions = pseudocode.instructions.toHashSet()
        val exitInstruction = pseudocode.exitInstruction
        for (previousInstruction in exitInstruction.previousInstructions) {
            previousInstruction.accept(object : InstructionVisitor() {
                override fun visitReturnValue(instruction: ReturnValueInstruction) {
                    if (instructions.contains(instruction)) { //exclude non-local return expressions
                        returnedExpressions.add(instruction.element)
                    }
                }

                override fun visitReturnNoValue(instruction: ReturnNoValueInstruction) {
                    if (instructions.contains(instruction)) {
                        returnedExpressions.add(instruction.element)
                    }
                }


                override fun visitJump(instruction: AbstractJumpInstruction) {
                    // Nothing
                }

                override fun visitUnconditionalJump(instruction: UnconditionalJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                private fun redirectToPrevInstructions(instruction: Instruction) {
                    for (redirectInstruction in instruction.previousInstructions) {
                        redirectInstruction.accept(this)
                    }
                }

                override fun visitNondeterministicJump(instruction: NondeterministicJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitMarkInstruction(instruction: MarkInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitInstruction(instruction: Instruction) {
                    if (instruction is KtElementInstruction) {
                        returnedExpressions.add(instruction.element)
                    }
                    else {
                        throw IllegalStateException("$instruction precedes the exit point")
                    }
                }
            })
        }
    }

    private fun checkLocalFunctions() {
        for (localDeclarationInstruction in pseudocode.localDeclarations) {
            val element = localDeclarationInstruction.element
            if (element is KtDeclarationWithBody) {

                val functionDescriptor = trace.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element) as? CallableDescriptor
                val expectedType = functionDescriptor?.returnType

                val providerForLocalDeclaration = ControlFlowInformationProvider(element, trace, localDeclarationInstruction.body)

                providerForLocalDeclaration.checkFunction(expectedType)
            }
        }
    }

    private fun checkDefiniteReturn(expectedReturnType: KotlinType, unreachableCode: UnreachableCode) {
        val function = subroutine as? KtDeclarationWithBody
                       ?: throw AssertionError("checkDefiniteReturn is called for ${subroutine.text} which is not KtDeclarationWithBody")

        if (!function.hasBody()) return

        val returnedExpressions = arrayListOf<KtElement>()
        collectReturnExpressions(returnedExpressions)

        val blockBody = function.hasBlockBody()

        var noReturnError = false
        for (returnedExpression in returnedExpressions) {
            returnedExpression.accept(object : KtVisitorVoid() {
                override fun visitReturnExpression(expression: KtReturnExpression) {
                    if (!blockBody) {
                        trace.report(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY.on(expression))
                    }
                }

                override fun visitKtElement(element: KtElement) {
                    if (!(element is KtExpression || element is KtWhenCondition)) return

                    if (blockBody && !noExpectedType(expectedReturnType)
                        && !KotlinBuiltIns.isUnit(expectedReturnType)
                        && !unreachableCode.elements.contains(element)) {
                        noReturnError = true
                    }
                }
            })
        }
        if (noReturnError) {
            trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.on(function))
        }
    }

    private fun reportUnreachableCode(unreachableCode: UnreachableCode) {
        for (element in unreachableCode.elements) {
            trace.report(Errors.UNREACHABLE_CODE.on(element, unreachableCode.getUnreachableTextRanges(element)))
            trace.record(BindingContext.UNREACHABLE_CODE, element, true)
        }
    }

    private fun collectUnreachableCode(): UnreachableCode {
        val reachableElements = hashSetOf<KtElement>()
        val unreachableElements = hashSetOf<KtElement>()
        for (instruction in pseudocode.instructionsIncludingDeadCode) {
            if (instruction !is KtElementInstruction
                || instruction is LoadUnitValueInstruction
                || instruction is MergeInstruction
                || instruction is MagicInstruction && instruction.synthetic)
                continue

            val element = instruction.element

            if (instruction is JumpInstruction) {
                val isJumpElement = element is KtBreakExpression
                                    || element is KtContinueExpression
                                    || element is KtReturnExpression
                                    || element is KtThrowExpression
                if (!isJumpElement) continue
            }

            if (instruction.dead) {
                unreachableElements.add(element)
            }
            else {
                reachableElements.add(element)
            }
        }
        return UnreachableCodeImpl(reachableElements, unreachableElements)
    }

    ////////////////////////////////////////////////////////////////////////////////
    //  Uninitialized variables analysis

    private fun markUninitializedVariables() {
        val varWithUninitializedErrorGenerated = hashSetOf<VariableDescriptor>()
        val varWithValReassignErrorGenerated = hashSetOf<VariableDescriptor>()
        val processClassOrObject = subroutine is KtClassOrObject

        val initializers = pseudocodeVariablesData.variableInitializers
        val declaredVariables = pseudocodeVariablesData.getDeclaredVariables(pseudocode, true)
        val blockScopeVariableInfo = pseudocodeVariablesData.blockScopeVariableInfo

        val reportedDiagnosticMap = hashMapOf<Instruction, DiagnosticFactory<*>>()

        pseudocode.traverse(TraversalOrder.FORWARD, initializers) {
            instruction: Instruction,
            enterData: Map<VariableDescriptor, VariableControlFlowState>,
            exitData: Map<VariableDescriptor, VariableControlFlowState> ->

            val ctxt = VariableInitContext(instruction, reportedDiagnosticMap, enterData, exitData, blockScopeVariableInfo)
            if (ctxt.variableDescriptor == null) return@traverse
            if (instruction is ReadValueInstruction) {
                val element = instruction.element
                if (PseudocodeUtil.isThisOrNoDispatchReceiver(instruction, trace.bindingContext)
                    && declaredVariables.contains(ctxt.variableDescriptor)) {
                    checkIsInitialized(ctxt, element, varWithUninitializedErrorGenerated)
                }
                return@traverse
            }
            if (instruction !is WriteValueInstruction) return@traverse
            val element = instruction.lValue as? KtExpression ?: return@traverse
            var error = checkValReassignment(ctxt, element, instruction,
                                             varWithValReassignErrorGenerated)
            if (!error && processClassOrObject) {
                error = checkAssignmentBeforeDeclaration(ctxt, element)
            }
            if (!error && processClassOrObject) {
                checkInitializationForCustomSetter(ctxt, element)
            }
        }
    }

    private fun recordInitializedVariables() {
        val pseudocode = pseudocodeVariablesData.pseudocode
        val initializers = pseudocodeVariablesData.variableInitializers
        recordInitializedVariables(pseudocode, initializers)
        for (instruction in pseudocode.localDeclarations) {
            recordInitializedVariables(instruction.body, initializers)
        }
    }

    private fun PropertyDescriptor.isDefinitelyInitialized(): Boolean {
        if (trace.get(BACKING_FIELD_REQUIRED, this) ?: false) return false
        val property = DescriptorToSourceUtils.descriptorToDeclaration(this)
        if (property is KtProperty && property.hasDelegate()) return false
        return true
    }

    private fun checkIsInitialized(
            ctxt: VariableInitContext,
            element: KtElement,
            varWithUninitializedErrorGenerated: MutableCollection<VariableDescriptor>
    ) {
        if (element !is KtSimpleNameExpression) return

        var isDefinitelyInitialized = ctxt.exitInitState?.definitelyInitialized() ?: false
        val variableDescriptor = ctxt.variableDescriptor
        if (!isDefinitelyInitialized && variableDescriptor is PropertyDescriptor) {
            isDefinitelyInitialized = variableDescriptor.isDefinitelyInitialized()
        }
        if (!isDefinitelyInitialized && !varWithUninitializedErrorGenerated.contains(variableDescriptor)) {
            if (variableDescriptor !is PropertyDescriptor) {
                variableDescriptor?.let { varWithUninitializedErrorGenerated.add(it) }
            }
            else if (variableDescriptor.isLateInit) {
                trace.record(MUST_BE_LATEINIT, variableDescriptor)
                return
            }
            when (variableDescriptor) {
                is ValueParameterDescriptor ->
                    report(Errors.UNINITIALIZED_PARAMETER.on(element, variableDescriptor), ctxt)
                is FakeCallableDescriptorForObject -> {
                    val classDescriptor = variableDescriptor.classDescriptor
                    when (classDescriptor.kind) {
                        ClassKind.ENUM_ENTRY ->
                            report(Errors.UNINITIALIZED_ENUM_ENTRY.on(element, classDescriptor), ctxt)
                        ClassKind.OBJECT -> if (classDescriptor.isCompanionObject) {
                            val container = classDescriptor.containingDeclaration
                            if (container is ClassDescriptor && container.kind == ClassKind.ENUM_CLASS) {
                                report(Errors.UNINITIALIZED_ENUM_COMPANION.on(element, container), ctxt)
                            }
                        }
                        else -> {}
                    }
                }
                is VariableDescriptor ->
                    report(Errors.UNINITIALIZED_VARIABLE.on(element, variableDescriptor), ctxt)
            }
        }
    }

    private fun getDeclarationDescriptor(declaration: KtDeclaration?): DeclarationDescriptor? {
        val descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
        return if (descriptor is ClassDescriptor) {
            // For a class primary constructor, we cannot directly get ConstructorDescriptor by KtClassInitializer,
            // so we have to do additional conversion: KtClassInitializer -> KtClassOrObject -> ClassDescriptor -> ConstructorDescriptor
            descriptor.unsubstitutedPrimaryConstructor
        }
        else {
            descriptor
        }
    }

    private fun isCapturedWrite(
            variableDescriptor: VariableDescriptor,
            writeValueInstruction: WriteValueInstruction
    ): Boolean {
        val containingDeclarationDescriptor = variableDescriptor.containingDeclaration
        // Do not consider member / top-level properties
        if (containingDeclarationDescriptor is ClassOrPackageFragmentDescriptor) return false
        var parentDeclaration = getElementParentDeclaration(writeValueInstruction.element)
        while (true) {
            val parentDescriptor = getDeclarationDescriptor(parentDeclaration)
            if (containingDeclarationDescriptor == parentDescriptor) {
                return false
            }
            else if (parentDeclaration is KtObjectDeclaration) {
                // anonymous object counts here the same as its owner
                parentDeclaration = getElementParentDeclaration(parentDeclaration)
            }
            else {
                return true
            }
        }
    }

    private fun checkValReassignment(
            ctxt: VariableInitContext,
            expression: KtExpression,
            writeValueInstruction: WriteValueInstruction,
            varWithValReassignErrorGenerated: MutableCollection<VariableDescriptor>
    ): Boolean {
        val variableDescriptor = ctxt.variableDescriptor
        val propertyDescriptor = variableDescriptor?.referencedProperty
        if (KtPsiUtil.isBackingFieldReference(variableDescriptor) && propertyDescriptor != null) {
            val accessor = PsiTreeUtil.getParentOfType(expression, KtPropertyAccessor::class.java)
            if (accessor != null) {
                val accessorDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, accessor)
                if (propertyDescriptor.getter === accessorDescriptor) {
                    //val can be reassigned through backing field inside its own getter
                    return false
                }
            }
        }

        val mayBeInitializedNotHere = ctxt.enterInitState?.mayBeInitialized() ?: false
        val hasBackingField = (variableDescriptor as? PropertyDescriptor)?.let {
            trace.get(BindingContext.BACKING_FIELD_REQUIRED, it)
        } ?: true
        if (variableDescriptor is PropertyDescriptor && variableDescriptor.isVar) {
            val descriptor = BindingContextUtils.getEnclosingDescriptor(trace.bindingContext, expression)
            val setterDescriptor = variableDescriptor.setter

            val receiverValue = expression.getResolvedCall(trace.bindingContext)?.getDispatchReceiverWithSmartCast()

            if (Visibilities.isVisible(receiverValue, variableDescriptor, descriptor)
                && setterDescriptor != null
                && !Visibilities.isVisible(receiverValue, setterDescriptor, descriptor)) {
                report(Errors.INVISIBLE_SETTER.on(expression, variableDescriptor, setterDescriptor.visibility,
                                                  setterDescriptor), ctxt)
                return true
            }
        }
        val isThisOrNoDispatchReceiver = PseudocodeUtil.isThisOrNoDispatchReceiver(writeValueInstruction, trace.bindingContext)
        val captured = variableDescriptor?.let { isCapturedWrite(it, writeValueInstruction) } ?: false
        if ((mayBeInitializedNotHere || !hasBackingField || !isThisOrNoDispatchReceiver || captured) &&
            variableDescriptor != null && !variableDescriptor.isVar) {
            var hasReassignMethodReturningUnit = false
            val parent = expression.parent
            val operationReference =
                    when (parent) {
                        is KtBinaryExpression -> parent.operationReference
                        is KtUnaryExpression -> parent.operationReference
                        else -> null
                    }
            if (operationReference != null) {
                val descriptor = trace.get(BindingContext.REFERENCE_TARGET, operationReference)
                if (descriptor is FunctionDescriptor) {
                    if (descriptor.returnType?.let { KotlinBuiltIns.isUnit(it) } ?: false) {
                        hasReassignMethodReturningUnit = true
                    }
                }
                if (descriptor == null) {
                    val descriptors = trace.get(BindingContext.AMBIGUOUS_REFERENCE_TARGET, operationReference) ?: emptyList()
                    for (referenceDescriptor in descriptors) {
                        if ((referenceDescriptor as? FunctionDescriptor)?.returnType?.let { KotlinBuiltIns.isUnit(it) } ?: false) {
                            hasReassignMethodReturningUnit = true
                        }
                    }
                }
            }
            if (!hasReassignMethodReturningUnit) {
                if (!isThisOrNoDispatchReceiver || !varWithValReassignErrorGenerated.contains(variableDescriptor)) {
                    if (captured && !mayBeInitializedNotHere && hasBackingField && isThisOrNoDispatchReceiver) {
                        report(Errors.CAPTURED_VAL_INITIALIZATION.on(expression, variableDescriptor), ctxt)
                    }
                    else {
                        report(Errors.VAL_REASSIGNMENT.on(expression, variableDescriptor), ctxt)
                    }
                }
                if (isThisOrNoDispatchReceiver) {
                    // try to get rid of repeating VAL_REASSIGNMENT diagnostic only for vars with no receiver
                    // or when receiver is this
                    varWithValReassignErrorGenerated.add(variableDescriptor)
                }
                return true
            }
        }
        return false
    }

    private fun checkAssignmentBeforeDeclaration(ctxt: VariableInitContext, expression: KtExpression) =
            if (ctxt.enterInitState?.isDeclared ?: false
                || ctxt.exitInitState?.isDeclared ?: false
                || ctxt.enterInitState?.mayBeInitialized() ?: false
                || !(ctxt.exitInitState?.mayBeInitialized() ?: false)) {
                false
            }
            else {
                if (ctxt.variableDescriptor != null) {
                    report(Errors.INITIALIZATION_BEFORE_DECLARATION.on(expression, ctxt.variableDescriptor), ctxt)
                }
                true
            }

    private fun checkInitializationForCustomSetter(ctxt: VariableInitContext, expression: KtExpression): Boolean {
        val variableDescriptor = ctxt.variableDescriptor
        if (variableDescriptor !is PropertyDescriptor
            || ctxt.enterInitState?.mayBeInitialized() ?: false
            || !(ctxt.exitInitState?.mayBeInitialized() ?: false)
            || !variableDescriptor.isVar
            || !(trace.get(BindingContext.BACKING_FIELD_REQUIRED, variableDescriptor) ?: false)) {
            return false
        }

        val property = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor) as? KtProperty
        ?: throw AssertionError("$variableDescriptor is not related to KtProperty")
        val setter = property.setter
        if (variableDescriptor.modality == Modality.FINAL && (setter == null || !setter.hasBody())) {
            return false
        }

        val variable = if (expression is KtDotQualifiedExpression &&
                           expression.receiverExpression is KtThisExpression) {
            expression.selectorExpression
        }
        else {
            expression
        }
        if (variable is KtSimpleNameExpression) {
            trace.record(IS_UNINITIALIZED, variableDescriptor)
            return true
        }
        return false
    }

    private fun recordInitializedVariables(
            pseudocode: Pseudocode,
            initializersMap: Map<Instruction, Edges<InitControlFlowInfo>>
    ) {
        val initializers = initializersMap[pseudocode.exitInstruction] ?: return
        val declaredVariables = pseudocodeVariablesData.getDeclaredVariables(pseudocode, false)
        for (variable in declaredVariables) {
            if (variable is PropertyDescriptor) {
                if (initializers.incoming[variable]?.definitelyInitialized() ?: false) continue
                trace.record(BindingContext.IS_UNINITIALIZED, variable)
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    //  "Unused variable" & "unused value" analyses

    private fun markUnusedVariables() {
        val variableStatusData = pseudocodeVariablesData.variableUseStatusData
        val reportedDiagnosticMap = hashMapOf<Instruction, DiagnosticFactory<*>>()
        val unusedValueExpressions = hashMapOf<KtExpression, Pair<VariableDescriptor, VariableUseContext>>()
        val usedValueExpressions = hashSetOf<KtExpression>()
        pseudocode.traverse(TraversalOrder.BACKWARD, variableStatusData) {
            instruction: Instruction,
            enterData: Map<VariableDescriptor, VariableUseState>,
            exitData: Map<VariableDescriptor, VariableUseState> ->

            val ctxt = VariableUseContext(instruction, reportedDiagnosticMap)
            val declaredVariables = pseudocodeVariablesData.getDeclaredVariables(instruction.owner, false)
            val variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(
                    instruction, trace.bindingContext)
            if (variableDescriptor == null
                || !declaredVariables.contains(variableDescriptor)
                || !ExpressionTypingUtils.isLocal(variableDescriptor.containingDeclaration, variableDescriptor)) {
                return@traverse
            }
            val variableUseState = enterData[variableDescriptor]
            when (instruction) {
                is WriteValueInstruction -> {
                    if (trace.get(CAPTURED_IN_CLOSURE, variableDescriptor) != null) return@traverse
                    val expressionInQuestion = instruction.element as? KtExpression ?: return@traverse
                    if (variableUseState != READ) {
                        unusedValueExpressions.put(expressionInQuestion, variableDescriptor to ctxt)
                    }
                    else {
                        usedValueExpressions.add(expressionInQuestion)
                    }
                }
                is VariableDeclarationInstruction -> {
                    val element = instruction.variableDeclarationElement as? KtNamedDeclaration ?: return@traverse
                    element.nameIdentifier ?: return@traverse
                    if (!VariableUseState.isUsed(variableUseState)) {
                        if (KtPsiUtil.isRemovableVariableDeclaration(element)) {
                            report(Errors.UNUSED_VARIABLE.on(element, variableDescriptor), ctxt)
                        }
                        else if (element is KtParameter) {
                            val owner = element.parent?.parent
                            when (owner) {
                                is KtPrimaryConstructor -> if (!element.hasValOrVar()) {
                                    val containingClass = owner.getContainingClassOrObject()
                                    val containingClassDescriptor = trace.get(
                                            BindingContext.DECLARATION_TO_DESCRIPTOR, containingClass)
                                    if (!DescriptorUtils.isAnnotationClass(containingClassDescriptor)) {
                                        report(Errors.UNUSED_PARAMETER.on(element, variableDescriptor), ctxt)
                                    }
                                }
                                is KtFunction -> {
                                    val mainFunctionDetector = MainFunctionDetector(trace.bindingContext)
                                    val isMain = owner is KtNamedFunction && mainFunctionDetector.isMain(owner)
                                    if (owner is KtFunctionLiteral) return@traverse
                                    val functionDescriptor =
                                            trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, owner) as? FunctionDescriptor
                                            ?: throw AssertionError(owner.text)
                                    val functionName = functionDescriptor.name.asString()
                                    if (isMain
                                        || functionDescriptor.isOverridableOrOverrides
                                        || owner.hasModifier(KtTokens.OVERRIDE_KEYWORD)
                                        || "getValue" == functionName
                                        || "setValue" == functionName
                                        || "propertyDelegated" == functionName) {
                                        return@traverse
                                    }
                                    report(Errors.UNUSED_PARAMETER.on(element, variableDescriptor), ctxt)
                                }
                            }
                        }
                    }
                    else if (variableUseState === ONLY_WRITTEN_NEVER_READ && KtPsiUtil.isRemovableVariableDeclaration(element)) {
                        report(Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE.on(element, variableDescriptor), ctxt)
                    }
                    else if (variableUseState === WRITTEN_AFTER_READ && element is KtVariableDeclaration) {
                        when (element) {
                            is KtProperty ->
                                element.initializer?.let {
                                    report(Errors.VARIABLE_WITH_REDUNDANT_INITIALIZER.on(it, variableDescriptor), ctxt)
                                }
                            is KtDestructuringDeclarationEntry ->
                                report(VARIABLE_WITH_REDUNDANT_INITIALIZER.on(element, variableDescriptor), ctxt)
                        }
                    }
                }
            }
        }
        unusedValueExpressions.keys.removeAll(usedValueExpressions)
        for ((expressionInQuestion, variableInContext) in unusedValueExpressions) {
            val (variableDescriptor, ctxt) = variableInContext
            when (expressionInQuestion) {
                is KtBinaryExpression -> if (expressionInQuestion.operationToken === KtTokens.EQ) {
                    expressionInQuestion.right?.let {
                        report(Errors.UNUSED_VALUE.on(expressionInQuestion, it, variableDescriptor), ctxt)
                    }
                }
                is KtPostfixExpression -> {
                    val operationToken = expressionInQuestion.operationReference.getReferencedNameElementType()
                    if (operationToken === KtTokens.PLUSPLUS || operationToken === KtTokens.MINUSMINUS) {
                        report(Errors.UNUSED_CHANGED_VALUE.on(expressionInQuestion, expressionInQuestion), ctxt)
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    //  "Unused expressions" in block

    private fun markUnusedExpressions() {
        val reportedDiagnosticMap = hashMapOf<Instruction, DiagnosticFactory<*>>()
        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            if (instruction !is KtElementInstruction) return@traverse

            val element = instruction.element as? KtExpression ?: return@traverse

            if (element.isUsedAsStatement(trace.bindingContext) && instruction.sideEffectFree) {
                val context = VariableContext(instruction, reportedDiagnosticMap)
                report(when (element) {
                           is KtLambdaExpression -> Errors.UNUSED_LAMBDA_EXPRESSION.on(element)
                           else -> Errors.UNUSED_EXPRESSION.on(element)
                       }, context)
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Statements

    private fun markStatements() = pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
        val value = (instruction as? InstructionWithValue)?.outputValue
        val pseudocode = instruction.owner
        val usages = pseudocode.getUsages(value)
        val isUsedAsExpression = !usages.isEmpty()
        val isUsedAsResultOfLambda = isUsedAsResultOfLambda(usages)
        for (element in pseudocode.getValueElements(value)) {
            trace.record(BindingContext.USED_AS_EXPRESSION, element, isUsedAsExpression)
            trace.record(BindingContext.USED_AS_RESULT_OF_LAMBDA, element, isUsedAsResultOfLambda)
        }
    }

    private fun checkIfExpressions() = pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
        val value = (instruction as? InstructionWithValue)?.outputValue
        for (element in instruction.owner.getValueElements(value)) {
            if (element !is KtIfExpression) continue

            if (element.isUsedAsExpression(trace.bindingContext)) {
                val thenExpression = element.then
                val elseExpression = element.`else`

                if (thenExpression == null || elseExpression == null) {
                    trace.report(INVALID_IF_AS_EXPRESSION.on(element))
                }
                else {
                    checkImplicitCastOnConditionalExpression(element)
                }
            }
        }
    }

    private fun checkImplicitCastOnConditionalExpression(expression: KtExpression) {
        val branchExpressions = collectResultingExpressionsOfConditionalExpression(expression)

        val expectedExpressionType = trace.get(EXPECTED_EXPRESSION_TYPE, expression)
        if (expectedExpressionType != null && expectedExpressionType !== DONT_CARE) return

        val expressionType = trace.getType(expression) ?: return
        if (KotlinBuiltIns.isAnyOrNullableAny(expressionType)) {
            val isUsedAsResultOfLambda = expression.isUsedAsResultOfLambda(trace.bindingContext)
            for (branchExpression in branchExpressions) {
                val branchType = trace.getType(branchExpression) ?: return
                if (KotlinBuiltIns.isAnyOrNullableAny(branchType) ||
                    isUsedAsResultOfLambda && KotlinBuiltIns.isUnitOrNullableUnit(branchType)) {
                    return
                }
            }
            for (branchExpression in branchExpressions) {
                val branchType = trace.getType(branchExpression) ?: continue
                if (KotlinBuiltIns.isNothing(branchType)) continue
                trace.report(IMPLICIT_CAST_TO_ANY.on(getResultingExpression(branchExpression), branchType, expressionType))
            }
        }
    }

    private fun checkWhenExpressions() {
        val initializers = pseudocodeVariablesData.variableInitializers
        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            if (instruction is MagicInstruction) {
                if (instruction.kind === MagicKind.EXHAUSTIVE_WHEN_ELSE) {
                    val next = instruction.next
                    if (next is MergeInstruction) {
                        val mergeInfo = initializers[next]?.incoming
                        val magicInfo = initializers[instruction]?.outgoing
                        if (mergeInfo != null && magicInfo != null) {
                            if (next.element is KtWhenExpression && magicInfo.checkDefiniteInitializationInWhen(mergeInfo)) {
                                trace.record(IMPLICIT_EXHAUSTIVE_WHEN, next.element)
                            }
                        }
                    }
                }
            }
            val value = (instruction as? InstructionWithValue)?.outputValue
            for (element in instruction.owner.getValueElements(value)) {
                if (element !is KtWhenExpression) continue

                if (element.isUsedAsExpression(trace.bindingContext)) {
                    checkImplicitCastOnConditionalExpression(element)
                }

                if (element.elseExpression != null) continue

                val context = trace.bindingContext
                val necessaryCases = WhenChecker.getNecessaryCases(element, context)
                if (!necessaryCases.isEmpty()) {
                    trace.report(NO_ELSE_IN_WHEN.on(element, necessaryCases))
                }
                else {
                    val subjectExpression = element.subjectExpression
                    if (subjectExpression != null) {
                        val enumClassDescriptor = WhenChecker.getClassDescriptorOfTypeIfEnum(trace.getType(subjectExpression))
                        if (enumClassDescriptor != null) {
                            val missingCases = WhenChecker.getEnumMissingCases(element, context, enumClassDescriptor)
                            if (!missingCases.isEmpty()) {
                                trace.report(NON_EXHAUSTIVE_WHEN.on(element, missingCases))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkConstructorConsistency() {
        when (subroutine) {
            is KtClassOrObject -> ConstructorConsistencyChecker.check(subroutine, trace, pseudocode, pseudocodeVariablesData)
            is KtSecondaryConstructor -> ConstructorConsistencyChecker.check(subroutine, trace, pseudocode, pseudocodeVariablesData)
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Tail calls

    private fun markTailCalls() {
        val subroutineDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, subroutine) as? FunctionDescriptor ?: return
        if (!subroutineDescriptor.isTailrec) return

        // finally blocks are copied which leads to multiple diagnostics reported on one instruction
        class KindAndCall(var kind: TailRecursionKind, internal val call: ResolvedCall<*>)

        val calls = HashMap<KtElement, KindAndCall>()
        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->

            if (instruction !is CallInstruction) return@traverse
            val resolvedCall = instruction.element.getResolvedCall(trace.bindingContext) ?: return@traverse

            // is this a recursive call?
            val functionDescriptor = resolvedCall.resultingDescriptor
            if (functionDescriptor.original != subroutineDescriptor) return@traverse
            // Overridden functions using default arguments at tail call are not included: KT-4285
            if (resolvedCall.call.valueArguments.size != functionDescriptor.valueParameters.size
                && !functionDescriptor.overriddenDescriptors.isEmpty())
                return@traverse

            val element = instruction.element
            //noinspection unchecked
            val parent = PsiTreeUtil.getParentOfType(
                    element,
                    KtTryExpression::class.java, KtFunction::class.java, KtAnonymousInitializer::class.java
            )

            if (parent is KtTryExpression) {
                // We do not support tail calls Collections.singletonMap() try-catch-finally, for simplicity of the mental model
                // very few cases there would be real tail-calls, and it's often not so easy for the user to see why
                calls.put(element, KindAndCall(IN_TRY, resolvedCall))
                return@traverse
            }

            val isTail = traverseFollowingInstructions(
                    instruction,
                    HashSet<Instruction>(),
                    TraversalOrder.FORWARD,
                    TailRecursionDetector(subroutine, instruction)
            )

            // A tail call is not allowed to change dispatch receiver
            //   class C {
            //       fun foo(other: C) {
            //           other.foo(this) // not a tail call
            //       }
            //   }
            val sameDispatchReceiver = resolvedCall.hasThisOrNoDispatchReceiver(trace.bindingContext)

            val kind = if (isTail && sameDispatchReceiver) TAIL_CALL else NON_TAIL

            val kindAndCall = calls[element]
            calls.put(element, KindAndCall(combineKinds(kind, kindAndCall?.kind), resolvedCall))
        }

        var hasTailCalls = false
        for ((element, kindAndCall) in calls) {
            when (kindAndCall.kind) {
                TAIL_CALL -> {
                    trace.record(TAIL_RECURSION_CALL, kindAndCall.call.call, TailRecursionKind.TAIL_CALL)
                    hasTailCalls = true
                }
                IN_TRY -> trace.report(Errors.TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED.on(element))
                NON_TAIL -> trace.report(Errors.NON_TAIL_RECURSIVE_CALL.on(element))
            }
        }

        if (!hasTailCalls && subroutine is KtNamedFunction) {
            trace.report(Errors.NO_TAIL_CALLS_FOUND.on(subroutine))
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Utility classes and methods

    /**
     * The method provides reporting of the same diagnostic only once for copied instructions
     * (depends on whether it should be reported for all or only for one of the copies)
     */
    private fun report(
            diagnostic: Diagnostic,
            ctxt: VariableContext
    ) {
        val instruction = ctxt.instruction
        if (instruction.copies.isEmpty()) {
            trace.report(diagnostic)
            return
        }
        val previouslyReported = ctxt.reportedDiagnosticMap
        previouslyReported.put(instruction, diagnostic.factory)

        var alreadyReported = false
        var sameErrorForAllCopies = true
        for (copy in instruction.copies) {
            val previouslyReportedErrorFactory = previouslyReported[copy]
            if (previouslyReportedErrorFactory != null) {
                alreadyReported = true
            }

            if (previouslyReportedErrorFactory !== diagnostic.factory) {
                sameErrorForAllCopies = false
            }
        }

        if (mustBeReportedOnAllCopies(diagnostic.factory)) {
            if (sameErrorForAllCopies) {
                trace.report(diagnostic)
            }
        }
        else {
            //only one reporting required
            if (!alreadyReported) {
                trace.report(diagnostic)
            }
        }
    }


    private open inner class VariableContext(
            internal val instruction: Instruction,
            internal val reportedDiagnosticMap: MutableMap<Instruction, DiagnosticFactory<*>>
    ) {
        internal val variableDescriptor = PseudocodeUtil.extractVariableDescriptorFromReference(instruction, trace.bindingContext)
    }

    private inner class VariableInitContext(
            instruction: Instruction,
            map: MutableMap<Instruction, DiagnosticFactory<*>>,
            `in`: Map<VariableDescriptor, VariableControlFlowState>,
            out: Map<VariableDescriptor, VariableControlFlowState>,
            blockScopeVariableInfo: BlockScopeVariableInfo
    ) : VariableContext(instruction, map) {
        internal val enterInitState = initialize(variableDescriptor, blockScopeVariableInfo, `in`)
        internal val exitInitState = initialize(variableDescriptor, blockScopeVariableInfo, out)

        private fun initialize(
                variableDescriptor: VariableDescriptor?,
                blockScopeVariableInfo: BlockScopeVariableInfo,
                map: Map<VariableDescriptor, VariableControlFlowState>
        ): VariableControlFlowState? {
            val state = map[variableDescriptor ?: return null]
            if (state != null) return state
            return PseudocodeVariablesData.getDefaultValueForInitializers(variableDescriptor, instruction, blockScopeVariableInfo)
        }
    }

    private inner class VariableUseContext(
            instruction: Instruction,
            map: MutableMap<Instruction, DiagnosticFactory<*>>
    ) : VariableContext(instruction, map)

    companion object {

        // Should return KtDeclarationWithBody or KtClassOrObject
        fun getElementParentDeclaration(element: KtElement) =
                PsiTreeUtil.getParentOfType(element, KtDeclarationWithBody::class.java, KtClassOrObject::class.java)

        private fun isUsedAsResultOfLambda(usages: List<Instruction>): Boolean {
            for (usage in usages) {
                if (usage is ReturnValueInstruction) {
                    val returnElement = usage.element
                    val parentElement = returnElement.parent
                    if (returnElement !is KtReturnExpression &&
                        (parentElement !is KtDeclaration || parentElement is KtFunctionLiteral)) {
                        return true
                    }
                }
            }
            return false
        }

        private fun collectResultingExpressionsOfConditionalExpression(expression: KtExpression): List<KtExpression> {
            val leafBranches = ArrayList<KtExpression>()
            collectResultingExpressionsOfConditionalExpressionRec(expression, leafBranches)
            return leafBranches
        }

        private fun collectResultingExpressionsOfConditionalExpressionRec(
                expression: KtExpression?,
                resultingExpressions: MutableList<KtExpression>
        ) {
            when (expression) {
                is KtIfExpression -> {
                    collectResultingExpressionsOfConditionalExpressionRec(expression.then, resultingExpressions)
                    collectResultingExpressionsOfConditionalExpressionRec(expression.`else`, resultingExpressions)
                }
                is KtWhenExpression -> for (whenEntry in expression.entries) {
                    collectResultingExpressionsOfConditionalExpressionRec(whenEntry.expression, resultingExpressions)
                }
                is Any -> {
                    val resultingExpression = getResultingExpression(expression)
                    if (resultingExpression is KtIfExpression || resultingExpression is KtWhenExpression) {
                        collectResultingExpressionsOfConditionalExpressionRec(resultingExpression, resultingExpressions)
                    }
                    else {
                        resultingExpressions.add(resultingExpression)
                    }
                }
            }
        }

        private fun getResultingExpression(expression: KtExpression): KtExpression {
            var finger = expression
            while (true) {
                var deparenthesized = KtPsiUtil.deparenthesize(finger)
                deparenthesized = KtPsiUtil.getExpressionOrLastStatementInBlock(deparenthesized)
                if (deparenthesized == null || deparenthesized === finger) break
                finger = deparenthesized
            }
            return finger
        }

        private fun combineKinds(kind: TailRecursionKind, existingKind: TailRecursionKind?): TailRecursionKind {
            val resultingKind: TailRecursionKind
            if (existingKind == null || existingKind == kind) {
                resultingKind = kind
            }
            else {
                if (check(kind, existingKind, IN_TRY, TAIL_CALL)) {
                    resultingKind = IN_TRY
                }
                else if (check(kind, existingKind, IN_TRY, NON_TAIL)) {
                    resultingKind = IN_TRY
                }
                else {
                    // TAIL_CALL, NON_TAIL
                    resultingKind = NON_TAIL
                }
            }
            return resultingKind
        }

        private fun check(a: Any, b: Any, x: Any, y: Any) = a === x && b === y || a === y && b === x

        private fun mustBeReportedOnAllCopies(diagnosticFactory: DiagnosticFactory<*>) =
                diagnosticFactory === UNUSED_VARIABLE
                || diagnosticFactory === UNUSED_PARAMETER
                || diagnosticFactory === UNUSED_CHANGED_VALUE
    }
}
