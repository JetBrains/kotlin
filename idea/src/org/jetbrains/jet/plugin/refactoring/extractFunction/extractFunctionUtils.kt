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
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import java.util.HashMap
import org.jetbrains.jet.lang.psi.JetParameter
import com.intellij.util.containers.MultiMap
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle
import org.jetbrains.jet.lang.cfg.pseudocode.PseudocodeUtil
import org.jetbrains.jet.plugin.util.MaybeValue
import org.jetbrains.jet.plugin.util.Maybe
import org.jetbrains.jet.lang.psi.psiUtil.isInsideOf
import java.util.HashSet
import org.jetbrains.jet.lang.cfg.pseudocode.WriteValueInstruction
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.plugin.util.MaybeError
import org.jetbrains.jet.lang.psi.psiUtil.appendElement
import org.jetbrains.jet.plugin.refactoring.createTempCopy
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.psiUtil.prependElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.cfg.pseudocode.JetElementInstruction
import org.jetbrains.jet.lang.cfg.pseudocode.AbstractJumpInstruction
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.psi.JetThisExpression
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody
import org.jetbrains.jet.lang.cfg.pseudocode.Instruction
import java.util.ArrayList
import org.jetbrains.jet.lang.cfg.pseudocode.ReadValueInstruction
import org.jetbrains.jet.lang.cfg.pseudocode.CallInstruction
import org.jetbrains.jet.lang.types.CommonSupertypes
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.cfg.pseudocode.ReturnValueInstruction
import org.jetbrains.jet.lang.cfg.Label
import org.jetbrains.jet.lang.psi.JetReturnExpression
import org.jetbrains.jet.lang.psi.JetBreakExpression
import org.jetbrains.jet.lang.psi.JetContinueExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory.FunctionBuilder
import org.jetbrains.jet.plugin.refactoring.JetNameValidatorImpl
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil
import org.jetbrains.jet.lang.psi.JetThrowExpression
import org.jetbrains.jet.plugin.imports.canBeReferencedViaImport
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import org.jetbrains.jet.lang.cfg.pseudocode.LocalFunctionDeclarationInstruction
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.psi.JetSuperExpression
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetTreeVisitorVoid
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import com.intellij.refactoring.util.RefactoringUIUtil
import org.jetbrains.jet.lang.psi.psiUtil.replaced
import java.util.Collections
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.lang.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.plugin.caches.resolve.getLazyResolveSession
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor

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
        filter { localInstruction -> localInstruction.getNextInstructions().any { it !in this } }

private fun List<Instruction>.getResultType(bindingContext: BindingContext): JetType {
    fun instructionToType(instruction: Instruction): JetType? {
        return when (instruction) {
            is ReadValueInstruction -> {
                PseudocodeUtil.extractVariableDescriptorIfAny(instruction, true, bindingContext)?.getType()
            }
            is CallInstruction -> {
                instruction.resolvedCall.getResultingDescriptor()?.getReturnType()
            }
            is ReturnValueInstruction -> {
                val expression = (instruction.getElement() as JetReturnExpression).getReturnedExpression()
                bindingContext[BindingContext.EXPRESSION_TYPE, expression]
            }
            else -> null
        }
    }

    val resultTypes = map(::instructionToType).filterNotNull()
    return if (resultTypes.isNotEmpty()) CommonSupertypes.commonSupertype(resultTypes) else DEFAULT_RETURN_TYPE
}

private fun List<AbstractJumpInstruction>.checkEquivalence(checkPsi: Boolean): Boolean {
    if (mapTo(HashSet<Label?>()) { it.getTargetLabel() }.size > 1) return false
    return !checkPsi || mapTo(HashSet<String?>()) { it.getElement().getText() }.size <= 1
}

private fun List<Instruction>.analyzeControlFlow(
        bindingContext: BindingContext,
        parameters: Set<Parameter>,
        inferredType: JetType?
): Maybe<ControlFlow, String> {
    val outParameters = parameters.filterTo(HashSet<Parameter>()) { it.mirrorVarName != null }

    if (outParameters.size > 1) {
        val outValuesStr = outParameters.map { it.argumentText }.sort().makeString(separator = "\n")

        return MaybeError("${JetRefactoringBundle.message("selected.code.fragment.has.multiple.output.values")!!}\n$outValuesStr")
    }

    val exitPoints = getExitPoints()

    val valuedReturnExits = ArrayList<ReturnValueInstruction>()
    val defaultExits = ArrayList<Instruction>()
    val jumpExits = ArrayList<AbstractJumpInstruction>()
    exitPoints.forEach {
        when (it) {
            is ReturnValueInstruction -> valuedReturnExits.add(it)

            is AbstractJumpInstruction -> {
                val element = it.getElement()
                if (element is JetReturnExpression
                        || element is JetBreakExpression
                        || element is JetContinueExpression
                        || element is JetThrowExpression) {
                    jumpExits.add(it)
                }
                else {
                    defaultExits.add(it)
                }
            }

            else -> if (it !is LocalFunctionDeclarationInstruction) {
                defaultExits.add(it)
            }
        }
    }

    val builtins = KotlinBuiltIns.getInstance()
    val hasMeaningfulType = inferredType != null && !builtins.isUnit(inferredType) && !builtins.isNothing(inferredType)

    if (outParameters.isNotEmpty()) {
        if (hasMeaningfulType || valuedReturnExits.isNotEmpty() || jumpExits.isNotEmpty()) {
            return MaybeError(JetRefactoringBundle.message("selected.code.fragment.has.output.values.and.exit.points")!!)
        }

        return MaybeValue(ParameterUpdate(outParameters.first()))
    }

    val multipleExitsError =
            MaybeError<ControlFlow, String>(JetRefactoringBundle.message("selected.code.fragment.has.multiple.exit.points")!!)

    if (hasMeaningfulType) {
        if (valuedReturnExits.isNotEmpty() || jumpExits.isNotEmpty()) return multipleExitsError

        return MaybeValue(ExpressionEvaluation(inferredType!!))
    }

    if (valuedReturnExits.isNotEmpty()) {
        if (jumpExits.isNotEmpty()) return multipleExitsError

        if (defaultExits.isNotEmpty()) {
            if (valuedReturnExits.size != 1) return multipleExitsError

            val element = valuedReturnExits.first!!.getElement()
            return MaybeValue(ConditionalJump(listOf(element), element))
        }

        if (!valuedReturnExits.checkEquivalence(false)) return multipleExitsError
        return MaybeValue(ExpressionEvaluationWithCallSiteReturn(valuedReturnExits.getResultType(bindingContext)))
    }

    if (jumpExits.isNotEmpty()) {
        if (!jumpExits.checkEquivalence(true)) return multipleExitsError

        val elements = jumpExits.map { it.getElement() }
        if (defaultExits.isNotEmpty()) return MaybeValue(ConditionalJump(elements, elements.first!!))
        return MaybeValue(UnconditionalJump(elements, elements.first!!))
    }

    return MaybeValue(DefaultControlFlow)
}

private fun ExtractionData.createTemporaryCodeBlock(): JetBlockExpression {
    val position = nextSibling.getTextRange()!!.getStartOffset()
    val tmpFile = originalFile.createTempCopy { text ->
        StringBuilder(text).insert(position, "fun() {\n${getCodeFragmentText()}\n}\n").toString()
    }
    val tmpFunction = tmpFile.findElementAt(position)?.getParentByType(javaClass<JetNamedFunction>())!!
    return tmpFunction.getBodyExpression() as JetBlockExpression
}

private fun ExtractionData.inferParametersInfo(
        commonParent: PsiElement,
        localInstructions: List<Instruction>,
        bindingContext: BindingContext,
        resultType: JetType?,
        replacementMap: MutableMap<Int, Replacement>,
        parameters: MutableSet<Parameter>
): MaybeError<ExtractionDescriptor, String>? {
    val varNameValidator = JetNameValidatorImpl(
            commonParent.getParentByType(javaClass<JetExpression>()),
            originalElements.first,
            { it is VariableDescriptor }
    )
    val modifiedVarDescriptors = localInstructions.getModifiedVarDescriptors(bindingContext)

    val extractedDescriptorToParameter = HashMap<DeclarationDescriptor, Parameter>()
    val nonDenotableTypes = HashSet<JetType>()
    for (refInfo in getBrokenReferencesInfo(createTemporaryCodeBlock())) {
        val (originalRef, originalDeclaration, originalDescriptor, resolvedCall) = refInfo.resolveResult
        val ref = refInfo.refExpr

        val selector = (ref.getParent() as? JetCallExpression) ?: ref
        val superExpr = (selector.getParent() as? JetQualifiedExpression)?.getReceiverExpression() as? JetSuperExpression
        if (superExpr != null) {
            return MaybeError(JetRefactoringBundle.message("cannot.extract.super.call")!!)
        }

        val receiverArgument = resolvedCall?.getReceiverArgument()
        val receiver = when(receiverArgument) {
            ReceiverValue.NO_RECEIVER -> resolvedCall?.getThisObject()
            else -> receiverArgument
        } ?: ReceiverValue.NO_RECEIVER

        val thisDescriptor = (receiver as? ThisReceiver)?.getDeclarationDescriptor()
        val hasThisReceiver = thisDescriptor != null
        val thisExpr = ref.getParent() as? JetThisExpression

        val hasClassObjectReceiver = (thisDescriptor as? ClassDescriptor)?.getKind() == ClassKind.CLASS_OBJECT
        val classObjectClassDescriptor =
                if (hasClassObjectReceiver) thisDescriptor!!.getContainingDeclaration() as? ClassDescriptor else null

        if (classObjectClassDescriptor != null) {
            if (!classObjectClassDescriptor.canBeReferencedViaImport()) {
                return MaybeError(JetRefactoringBundle.message("cannot.extract.non.local.declaration.ref")!!)
            }

            replacementMap[refInfo.offsetInBody] = FqNameReplacement(DescriptorUtils.getFqNameSafe(originalDescriptor))
        }
        else {
            val extractThis = hasThisReceiver || thisExpr != null
            val extractLocalVar =
                    (originalDeclaration is JetProperty && originalDeclaration.isLocal()) || originalDeclaration is JetParameter

            val descriptorToExtract = (if (extractThis) thisDescriptor else null) ?: originalDescriptor

            val extractParameter = extractThis || extractLocalVar
            if (extractParameter) {
                val parameterType =
                        if (hasThisReceiver) {
                            when (descriptorToExtract) {
                                is ClassDescriptor -> descriptorToExtract.getDefaultType()
                                is CallableDescriptor -> descriptorToExtract.getReceiverParameter()?.getType()
                                else -> null
                            } ?: DEFAULT_PARAMETER_TYPE
                        }
                        else bindingContext[BindingContext.EXPRESSION_TYPE, originalRef] ?: DEFAULT_PARAMETER_TYPE
                if (!parameterType.canBeReferencedViaImport()) {
                    nonDenotableTypes.add(parameterType)
                    continue
                }

                val existingParameter = extractedDescriptorToParameter[descriptorToExtract]
                val parameter: Parameter =
                        if (existingParameter != null) {
                            if (!JetTypeChecker.INSTANCE.equalTypes(existingParameter.parameterType, parameterType)) {
                                val newParameter = existingParameter.copy(
                                        parameterType = CommonSupertypes.commonSupertype(listOf(existingParameter.parameterType, parameterType))
                                )

                                extractedDescriptorToParameter[descriptorToExtract] = newParameter

                                for ((offset, replacement) in replacementMap) {
                                    if (replacement is ParameterReplacement && replacement.parameter == existingParameter) {
                                        replacementMap[offset] = replacement.copy(newParameter)
                                    }
                                }

                                newParameter
                            }
                            else existingParameter
                        }
                        else {
                            val parameterName: String =
                                    if (extractThis) {
                                        JetNameSuggester.suggestNames(parameterType, varNameValidator, null).first()
                                    }
                                    else originalDeclaration.getName()!!

                            val mirrorVarName: String?
                            if (descriptorToExtract in modifiedVarDescriptors) {
                                mirrorVarName = varNameValidator.validateName(parameterName)
                            }
                            else {
                                mirrorVarName = null
                            }

                            val argumentText =
                                    if (hasThisReceiver && extractThis)
                                        "this@${parameterType.getConstructor().getDeclarationDescriptor()!!.getName().asString()}"
                                    else
                                        (thisExpr ?: ref).getText()!!

                            val parameter = Parameter(argumentText, parameterName, mirrorVarName, parameterType, extractThis)

                            extractedDescriptorToParameter[descriptorToExtract] = parameter

                            parameter
                        }

                replacementMap[refInfo.offsetInBody] =
                if (hasThisReceiver && extractThis) AddPrefixReplacement(parameter) else RenameReplacement(parameter)
            }
        }
    }

    if (resultType != null && !resultType.canBeReferencedViaImport()) {
        nonDenotableTypes.add(resultType)
    }

    if (nonDenotableTypes.isNotEmpty()) {
        val typeStr = nonDenotableTypes.map {
            DescriptorRenderer.HTML.renderType(it)
        }.sort().makeString(separator = "\n")

        return MaybeError("${JetRefactoringBundle.message("parameter.types.are.not.denotable")!!}\n$typeStr")
    }

    parameters.addAll(extractedDescriptorToParameter.values())

    return null
}

private fun ExtractionData.checkLocalDeclarationsWithNonLocalUsages(
        allInstructions: List<Instruction>,
        localInstructions: List<Instruction>,
        bindingContext: BindingContext
): MaybeError<ExtractionDescriptor, String>? {
    // todo: non-locally used declaration can be turned into the output value

    val declarations = ArrayList<JetNamedDeclaration>()
    for (instruction in allInstructions) {
        if (instruction in localInstructions) continue

        PseudocodeUtil.extractVariableDescriptorIfAny(instruction, true, bindingContext)?.let { descriptor ->
            val declaration = DescriptorToDeclarationUtil.getDeclaration(project, descriptor, bindingContext)
            if (declaration is JetNamedDeclaration && declaration.isInsideOf(originalElements)) {
                declarations.add(declaration)
            }
        }
    }

    if (declarations.isNotEmpty()) {
        val localVarStr = declarations.map { it.getName()!! }.sort().makeString(separator = "\n")
        return MaybeError("${JetRefactoringBundle.message("variables.are.used.outside.of.selected.code.fragment")!!}\n$localVarStr")
    }

    return null
}

private fun ExtractionData.checkDeclarationsMovingOutOfScope(controlFlow: ControlFlow): MaybeError<ExtractionDescriptor, String>? {
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
        val declStr = declarationsOutOfScope.map { it.getName()!! }.sort().makeString(separator = "\n")
        return MaybeError("${JetRefactoringBundle.message("declarations.will.move.out.of.scope")!!}\n$declStr")
    }

    return null
}

fun ExtractionData.performAnalysis(): Maybe<ExtractionDescriptor, String> {
    if (originalElements.empty) return MaybeError(JetRefactoringBundle.message("cannot.refactor.no.expresson")!!)

    val commonParent = PsiTreeUtil.findCommonParent(originalElements)!!

    val enclosingDeclaration = commonParent.getParentByType(javaClass<JetDeclarationWithBody>())
    if (enclosingDeclaration == null) return MaybeError(JetRefactoringBundle.message("cannot.refactor.no.container")!!)

    val resolveSession = originalFile.getLazyResolveSession()
    val bindingContext = resolveSession.resolveToElement(enclosingDeclaration.getBodyExpression())

    val pseudocode = PseudocodeUtil.generatePseudocode(enclosingDeclaration, bindingContext)
    val localInstructions = pseudocode.getInstructions().filter {
        it is JetElementInstruction && it.getElement().isInsideOf(originalElements)
    }

    val inferredResultType = getInferredResultType()

    val replacementMap = HashMap<Int, Replacement>()
    val parameters = HashSet<Parameter>()
    val parameterError =
            inferParametersInfo(commonParent, localInstructions, bindingContext, inferredResultType, replacementMap, parameters)
    if (parameterError != null) return parameterError

    val controlFlowInfo = localInstructions.analyzeControlFlow(bindingContext, parameters, inferredResultType)
    if (controlFlowInfo is MaybeError) return MaybeError(controlFlowInfo.error)
    val controlFlow = (controlFlowInfo as MaybeValue).value

    val localDeclError = checkLocalDeclarationsWithNonLocalUsages(pseudocode.getInstructions(), localInstructions, bindingContext)
    if (localDeclError != null) return localDeclError

    val outOfScopeError = checkDeclarationsMovingOutOfScope(controlFlow)
    if (outOfScopeError != null) return outOfScopeError

    val functionNameValidator = JetNameValidatorImpl(
            nextSibling.getParent(),
            nextSibling,
            {it is FunctionDescriptor || it is ClassDescriptor}
    )
    val functionName = JetNameSuggester.suggestNames(controlFlow.returnType, functionNameValidator, DEFAULT_FUNCTION_NAME).first()

    val receiverCandidates = parameters.filterTo(HashSet<Parameter>()) { it.receiverCandidate }
    val receiverParameter = if (receiverCandidates.size == 1) receiverCandidates.first() else null
    receiverParameter?.let { parameters.remove(it) }

    return MaybeValue(
            ExtractionDescriptor(this, functionName, "", parameters.sortBy { it.name }, receiverParameter, replacementMap, controlFlow)
    )
}

fun ExtractionDescriptor.validate(): ExtractionDescriptorWithConflicts {
    val conflicts = MultiMap<PsiElement, String>()

    val nameByOffset = HashMap<Int, JetElement>()
    val function = generateFunction(true, nameByOffset)

    val bindingContext = AnalyzerFacadeWithCache.getContextForElement(function.getBodyExpression()!!)

    for ((originalOffset, resolveResult) in extractionData.refOffsetToDeclaration) {
        if (resolveResult.declaration in extractionData.originalElements) continue

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
                        && !compareDescriptors(currentDescriptor, resolveResult.descriptor))) {
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

fun ExtractionDescriptor.getFunctionText(
        withBody: Boolean = true,
        descriptorRenderer: DescriptorRenderer = DescriptorRenderer.FQ_NAMES_IN_TYPES
): String {
    return FunctionBuilder().let { builder ->
        builder.modifier(visibility)

        receiverParameter?.let { builder.receiver(descriptorRenderer.renderType(it.parameterType)) } ?: builder.noReceiver()

        builder.name(name)

        parameters.forEach { parameter ->
            builder.param(parameter.name, descriptorRenderer.renderType(parameter.parameterType))
        }

        with(controlFlow.returnType) {
            if (isDefault()) builder.noReturnType() else builder.returnType(descriptorRenderer.renderType(this))
        }

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
                val position = nextSibling.getTextRange()!!.getStartOffset()
                val tmpFile = originalFile.createTempCopy { text ->
                    StringBuilder(text).insert(position, getFunctionText() + "\n").toString()
                }
                tmpFile.findElementAt(position)?.getParentByType(javaClass<JetNamedFunction>())!!
            }
            else {
                JetPsiFactory.createFunction(project, getFunctionText())
            }
        }
    }

    fun adjustFunctionBody(function: JetNamedFunction) {
        val body = function.getBodyExpression() as JetBlockExpression

        val exprReplacementMap = HashMap<JetElement, (JetElement) -> JetElement>()
        val originalOffsetByExpr = HashMap<JetElement, Int>()

        val range = body.getStatements().first?.getTextRange()
        if (range == null) return

        val bodyOffset = range.getStartOffset()
        val file = body.getContainingFile()!!

        for ((offsetInBody, resolveResult) in extractionData.refOffsetToDeclaration) {
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
                    val newExpr = it.replaced(JetPsiFactory.createReturn(project, it.getText()!!)).getReturnedExpression()!!
                    val counterpartMap = createNameCounterpartMap(it, newExpr)
                    nameByOffset.entrySet().forEach { it.setValue(counterpartMap[it.getValue()]!!) }
                }
        }
    }

    fun insertFunction(function: JetNamedFunction): JetNamedFunction {
        return with(extractionData) {
            val targetContainer = nextSibling.getParent()!!
            val functionInFile = targetContainer.addBefore(function, nextSibling) as JetNamedFunction
            targetContainer.addBefore(JetPsiFactory.createWhiteSpace(project, "\n\n"), nextSibling)

            functionInFile
        }
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
                .makeString(separator = ", ", prefix = "$name(", postfix = ")")
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
        anchor.replace(wrappedCall)

        return function
    }

    val function = createFunction()
    adjustFunctionBody(function)

    if (inTempFile) return function

    val functionInPlace = makeCall(insertFunction(function))
    ShortenReferences.process(functionInPlace)
    return functionInPlace
}