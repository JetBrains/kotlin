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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.BaseRefactoringProcessor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.core.refactoring.isMultiLine
import org.jetbrains.kotlin.idea.intentions.ConvertToExpressionBodyIntention
import org.jetbrains.kotlin.idea.intentions.InfixCallToOrdinaryIntention
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.*
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValueBoxer.AsTuple
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRange
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRange.Match
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.UnificationResult.StronglyMatched
import org.jetbrains.kotlin.idea.util.psi.patternMatching.UnificationResult.WeaklyMatched
import org.jetbrains.kotlin.idea.util.psi.patternMatching.UnifierParameter
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiFactory.CallableBuilder
import org.jetbrains.kotlin.psi.codeFragmentUtil.DEBUG_TYPE_REFERENCE_STRING
import org.jetbrains.kotlin.psi.codeFragmentUtil.debugTypeInfo
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.typeUtil.builtIns
import java.util.*

fun ExtractionGeneratorConfiguration.getDeclarationText(
        withBody: Boolean = true,
        descriptorRenderer: DescriptorRenderer = if (generatorOptions.flexibleTypesAllowed)
                                                    DescriptorRenderer.FLEXIBLE_TYPES_FOR_CODE
                                                 else IdeDescriptorRenderers.SOURCE_CODE
): String {
    val extractionTarget = generatorOptions.target
    if (!extractionTarget.isAvailable(descriptor)) {
        throw BaseRefactoringProcessor.ConflictsInTestsException(listOf("Can't generate ${extractionTarget.targetName}: ${descriptor.extractionData.codeFragmentText}"))
    }

    val builderTarget = when (extractionTarget) {
        ExtractionTarget.FUNCTION, ExtractionTarget.FAKE_LAMBDALIKE_FUNCTION -> CallableBuilder.Target.FUNCTION
        else -> CallableBuilder.Target.READ_ONLY_PROPERTY
    }
    return CallableBuilder(builderTarget).let { builder ->
        builder.modifier(descriptor.visibility)

        builder.typeParams(
                descriptor.typeParameters.map {
                    val typeParameter = it.originalDeclaration
                    val bound = typeParameter.extendsBound
                    typeParameter.name + (bound?.let { " : " + it.text } ?: "")
                }
        )

        fun KotlinType.typeAsString(): String {
            return if (descriptor.extractionData.options.allowSpecialClassNames && isSpecial()) DEBUG_TYPE_REFERENCE_STRING else descriptorRenderer.renderType(this)
        }

        descriptor.receiverParameter?.let {
            val receiverType = it.getParameterType(descriptor.extractionData.options.allowSpecialClassNames)
            val receiverTypeAsString = receiverType.typeAsString()
            val isFunctionType = KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(receiverType)
            builder.receiver(if (isFunctionType) "($receiverTypeAsString)" else receiverTypeAsString)
        }

        builder.name(if (descriptor.name == "" && generatorOptions.allowDummyName) "myFun" else descriptor.name)

        descriptor.parameters.forEach { parameter ->
            builder.param(parameter.name,
                          parameter.getParameterType(descriptor.extractionData.options.allowSpecialClassNames).typeAsString())
        }

        with(descriptor.returnType) {
            if (isDefault() || isError || extractionTarget == ExtractionTarget.PROPERTY_WITH_INITIALIZER) {
                builder.noReturnType()
            } else {
                builder.returnType(typeAsString())
            }
        }

        builder.typeConstraints(descriptor.typeParameters.flatMap { it.originalConstraints }.map { it.text!! })

        if (withBody) {
            val bodyText = descriptor.extractionData.codeFragmentText
            when (extractionTarget) {
                ExtractionTarget.FUNCTION,
                ExtractionTarget.FAKE_LAMBDALIKE_FUNCTION,
                ExtractionTarget.PROPERTY_WITH_GETTER -> builder.blockBody(bodyText)
                ExtractionTarget.PROPERTY_WITH_INITIALIZER -> builder.initializer(bodyText)
                ExtractionTarget.LAZY_PROPERTY -> builder.lazyBody(bodyText)
            }
        }

        builder.asString()
    }
}

fun KotlinType.isSpecial(): Boolean {
    val classDescriptor = this.constructor.declarationDescriptor as? ClassDescriptor ?: return false
    return classDescriptor.name.isSpecial || DescriptorUtils.isLocal(classDescriptor)
}

fun createNameCounterpartMap(from: KtElement, to: KtElement): Map<KtSimpleNameExpression, KtSimpleNameExpression> {
    val map = HashMap<KtSimpleNameExpression, KtSimpleNameExpression>()

    val fromOffset = from.textRange!!.startOffset
    from.accept(
            object : KtTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    val offset = expression.textRange!!.startOffset - fromOffset
                    val newExpression = to.findElementAt(offset)?.getNonStrictParentOfType<KtSimpleNameExpression>()
                    assert(newExpression != null) { "Couldn't find expression at $offset in '${to.text}'" }

                    map[expression] = newExpression!!
                }
            }
    )

    return map
}

class DuplicateInfo(
        val range: KotlinPsiRange,
        val controlFlow: ControlFlow,
        val arguments: List<String>
)

fun ExtractableCodeDescriptor.findDuplicates(): List<DuplicateInfo> {
    fun processWeakMatch(match: Match, newControlFlow: ControlFlow): Boolean {
        val valueCount = controlFlow.outputValues.size

        val weakMatches = HashMap((match.result as WeaklyMatched).weakMatches)
        val currentValuesToNew = HashMap<OutputValue, OutputValue>()

        fun matchValues(currentValue: OutputValue, newValue: OutputValue): Boolean {
            if ((currentValue is Jump) != (newValue is Jump)) return false
            if (currentValue.originalExpressions.zip(newValue.originalExpressions).all { weakMatches[it.first] == it.second }) {
                currentValuesToNew[currentValue] = newValue
                weakMatches.keys.removeAll(currentValue.originalExpressions)
                return true
            }
            return false
        }

        if (valueCount == 1) {
            matchValues(controlFlow.outputValues.first(), newControlFlow.outputValues.first())
        } else {
            outer@
            for (currentValue in controlFlow.outputValues)
                for (newValue in newControlFlow.outputValues) {
                    if ((currentValue is ExpressionValue) != (newValue is ExpressionValue)) continue
                    if (matchValues(currentValue, newValue)) continue@outer
                }
        }

        return currentValuesToNew.size == valueCount && weakMatches.isEmpty()
    }

    fun getControlFlowIfMatched(match: Match): ControlFlow? {
        val analysisResult = extractionData.copy(originalRange = match.range).performAnalysis()
        if (analysisResult.status != AnalysisResult.Status.SUCCESS) return null

        val newControlFlow = analysisResult.descriptor!!.controlFlow
        if (newControlFlow.outputValues.isEmpty()) return newControlFlow
        if (controlFlow.outputValues.size != newControlFlow.outputValues.size) return null

        val matched = when (match.result) {
            is StronglyMatched -> true
            is WeaklyMatched -> processWeakMatch(match, newControlFlow)
            else -> throw AssertionError("Unexpected unification result: ${match.result}")
        }

        return if (matched) newControlFlow else null
    }

    val unifierParameters = parameters.map { UnifierParameter(it.originalDescriptor, it.getParameterType(extractionData.options.allowSpecialClassNames)) }

    val unifier = KotlinPsiUnifier(unifierParameters, true)

    val scopeElement = getOccurrenceContainer() ?: return Collections.emptyList()
    val originalTextRange = extractionData.originalRange.getTextRange()
    return extractionData
            .originalRange
            .match(scopeElement, unifier)
            .asSequence()
            .filter { !(it.range.getTextRange() intersects originalTextRange) }
            .map { match ->
                val controlFlow = getControlFlowIfMatched(match)
                controlFlow?.let { DuplicateInfo(match.range, it, unifierParameters.map { match.result.substitution[it]!!.text!! }) }
            }
            .filterNotNull()
            .toList()
}

private fun ExtractableCodeDescriptor.getOccurrenceContainer(): PsiElement? {
    return extractionData.duplicateContainer ?: extractionData.targetSibling.parent
}

private fun makeCall(
        extractableDescriptor: ExtractableCodeDescriptor,
        declaration: KtNamedDeclaration,
        controlFlow: ControlFlow,
        rangeToReplace: KotlinPsiRange,
        arguments: List<String>) {
    fun insertCall(anchor: PsiElement, wrappedCall: KtExpression) {
        val firstExpression = rangeToReplace.elements.firstOrNull { it is KtExpression } as? KtExpression
        if (firstExpression?.isFunctionLiteralOutsideParentheses() ?: false) {
            val functionLiteralArgument = firstExpression?.getStrictParentOfType<KtFunctionLiteralArgument>()!!
            functionLiteralArgument.moveInsideParenthesesAndReplaceWith(wrappedCall, extractableDescriptor.originalContext)
            return
        }

        if (anchor is KtOperationReferenceExpression) {
            val operationExpression = anchor.parent as? KtOperationExpression ?: return
            val newNameExpression = when (operationExpression) {
                is KtUnaryExpression -> OperatorToFunctionIntention.convert(operationExpression).second
                is KtBinaryExpression -> {
                    InfixCallToOrdinaryIntention.convert(operationExpression).getCalleeExpressionIfAny()
                }
                else -> null
            }
            newNameExpression?.replace(wrappedCall)
            return
        }

        anchor.replace(wrappedCall)
    }

    if (rangeToReplace !is KotlinPsiRange.ListRange) return

    val anchor = rangeToReplace.startElement
    val anchorParent = anchor.parent!!

    anchor.nextSibling?.let { from ->
        val to = rangeToReplace.endElement
        if (to != anchor) {
            anchorParent.deleteChildRange(from, to);
        }
    }

    val calleeName = declaration.name
    val callText = when (declaration) {
        is KtNamedFunction -> {
            val argumentsText = arguments.joinToString(separator = ", ", prefix = "(", postfix = ")")
            val typeArguments = extractableDescriptor.typeParameters.map { it.originalDeclaration.name }
            val typeArgumentsText = with(typeArguments) {
                if (isNotEmpty()) joinToString(separator = ", ", prefix = "<", postfix = ">") else ""
            }
            "$calleeName$typeArgumentsText$argumentsText"
        }
        else -> calleeName
    }

    val anchorInBlock = sequence(anchor) { it.parent }.firstOrNull { it.parent is KtBlockExpression }
    val block = (anchorInBlock?.parent as? KtBlockExpression) ?: anchorParent

    val psiFactory = KtPsiFactory(anchor.project)
    val newLine = psiFactory.createNewLine()

    if (controlFlow.outputValueBoxer is AsTuple && controlFlow.outputValues.size > 1 && controlFlow.outputValues.all { it is Initializer }) {
        val declarationsToMerge = controlFlow.outputValues.map { (it as Initializer).initializedDeclaration }
        val isVar = declarationsToMerge.first().isVar
        if (declarationsToMerge.all { it.isVar == isVar }) {
            controlFlow.declarationsToCopy.subtract(declarationsToMerge).forEach {
                block.addBefore(psiFactory.createDeclaration<KtDeclaration>(it.text!!), anchorInBlock) as KtDeclaration
                block.addBefore(newLine, anchorInBlock)
            }

            val entries = declarationsToMerge.map { p -> p.name + (p.typeReference?.let { ": ${it.text}" } ?: "") }
            anchorInBlock?.replace(
                    psiFactory.createDeclaration("${if (isVar) "var" else "val"} (${entries.joinToString()}) = $callText")
            )

            return
        }
    }

    val inlinableCall = controlFlow.outputValues.size <= 1
    val unboxingExpressions =
            if (inlinableCall) {
                controlFlow.outputValueBoxer.getUnboxingExpressions(callText!!)
            }
            else {
                val varNameValidator = NewDeclarationNameValidator(block, anchorInBlock, NewDeclarationNameValidator.Target.VARIABLES)
                val resultVal = KotlinNameSuggester.suggestNamesByType(extractableDescriptor.returnType, varNameValidator, null).first()
                block.addBefore(psiFactory.createDeclaration("val $resultVal = $callText"), anchorInBlock)
                block.addBefore(newLine, anchorInBlock)
                controlFlow.outputValueBoxer.getUnboxingExpressions(resultVal)
            }

    val copiedDeclarations = HashMap<KtDeclaration, KtDeclaration>()
    for (decl in controlFlow.declarationsToCopy) {
        val declCopy = psiFactory.createDeclaration<KtDeclaration>(decl.text!!)
        copiedDeclarations[decl] = block.addBefore(declCopy, anchorInBlock) as KtDeclaration
        block.addBefore(newLine, anchorInBlock)
    }

    if (controlFlow.outputValues.isEmpty()) {
        anchor.replace(psiFactory.createExpression(callText!!))
        return
    }

    fun wrapCall(outputValue: OutputValue, callText: String): List<PsiElement> {
        return when (outputValue) {
            is OutputValue.ExpressionValue -> {
                val exprText = if (outputValue.callSiteReturn) {
                    val firstReturn = outputValue.originalExpressions.filterIsInstance<KtReturnExpression>().firstOrNull()
                    val label = firstReturn?.getTargetLabel()?.text ?: ""
                    "return$label $callText"
                }
                else {
                    callText
                }
                Collections.singletonList(psiFactory.createExpression(exprText))
            }

            is ParameterUpdate ->
                Collections.singletonList(
                        psiFactory.createExpression("${outputValue.parameter.argumentText} = $callText")
                )

            is Jump -> {
                if (outputValue.elementToInsertAfterCall == null) {
                    Collections.singletonList(psiFactory.createExpression(callText))
                }
                else if (outputValue.conditional) {
                    Collections.singletonList(
                            psiFactory.createExpression("if ($callText) ${outputValue.elementToInsertAfterCall.text}")
                    )
                }
                else {
                    listOf(
                            psiFactory.createExpression(callText),
                            newLine,
                            psiFactory.createExpression(outputValue.elementToInsertAfterCall.text!!)
                    )
                }
            }

            is Initializer -> {
                val newProperty = copiedDeclarations[outputValue.initializedDeclaration] as KtProperty
                newProperty.setInitializer(psiFactory.createExpression(callText))
                Collections.emptyList()
            }

            else -> throw IllegalArgumentException("Unknown output value: $outputValue")
        }
    }

    val defaultValue = controlFlow.defaultOutputValue

    controlFlow.outputValues
            .filter { it != defaultValue }
            .flatMap { wrapCall(it, unboxingExpressions[it]!!) }
            .withIndex()
            .forEach {
                val (i, e) = it

                if (i > 0) {
                    block.addBefore(newLine, anchorInBlock)
                }
                block.addBefore(e, anchorInBlock)
            }

    defaultValue?.let {
        if (!inlinableCall) {
            block.addBefore(newLine, anchorInBlock)
        }
        insertCall(anchor, wrapCall(it, unboxingExpressions[it]!!).first() as KtExpression)
    }

    if (anchor.isValid) {
        anchor.delete()
    }
}

fun ExtractionGeneratorConfiguration.generateDeclaration(
        declarationToReplace: KtNamedDeclaration? = null
): ExtractionResult{
    val psiFactory = KtPsiFactory(descriptor.extractionData.originalFile)
    val nameByOffset = HashMap<Int, KtElement>()

    fun createDeclaration(): KtNamedDeclaration {
        return with(descriptor.extractionData) {
            if (generatorOptions.inTempFile) {
                createTemporaryDeclaration("${getDeclarationText()}\n")
            }
            else {
                psiFactory.createDeclaration(getDeclarationText())
            }
        }
    }

    fun getReturnArguments(resultExpression: KtExpression?): List<String> {
        return descriptor.controlFlow.outputValues
                .map {
                    when (it) {
                        is ExpressionValue -> resultExpression?.text
                        is Jump -> if (it.conditional) "false" else null
                        is ParameterUpdate -> it.parameter.nameForRef
                        is Initializer -> it.initializedDeclaration.name
                        else -> throw IllegalArgumentException("Unknown output value: $it")
                    }
                }
                .filterNotNull()
    }

    fun replaceWithReturn(
            originalExpression: KtExpression,
            replacingExpression: KtReturnExpression,
            expressionToUnifyWith: KtExpression?
    ) {
        val currentResultExpression =
                (if (originalExpression is KtReturnExpression) originalExpression.returnedExpression else originalExpression) ?: return

        val newResultExpression = descriptor.controlFlow.defaultOutputValue?.let {
            val boxedExpression = originalExpression.replaced(replacingExpression).returnedExpression!!
            descriptor.controlFlow.outputValueBoxer.extractExpressionByValue(boxedExpression, it)
        }

        @Suppress
        if (newResultExpression == null) {
            throw AssertionError("Can' replace '${originalExpression.text}' with '${replacingExpression.text}'")
        }

        val counterpartMap = createNameCounterpartMap(currentResultExpression, expressionToUnifyWith ?: newResultExpression)
        nameByOffset.entries.forEach { e -> counterpartMap.getRaw(e.value)?.let { e.setValue(it) } }
    }

    fun getCounterparts<T : KtExpression>(originalExpressions: Collection<T>,
                                                                   body: KtExpression,
                                                                   bodyOffset: Int,
                                                                   file: PsiFile): List<T> {
        return originalExpressions.map { originalExpression ->
            val offsetInBody = originalExpression.textRange!!.startOffset - descriptor.extractionData.originalStartOffset!!
            file.findElementAt(bodyOffset + offsetInBody)?.getNonStrictParentOfType(originalExpression.javaClass)
            ?: throw AssertionError("Couldn't find expression at $offsetInBody in '${body.text}'")
        }
    }

    fun adjustDeclarationBody(declaration: KtNamedDeclaration) {
        val body = declaration.getGeneratedBody()

        val exprReplacementMap = HashMap<KtElement, (ExtractableCodeDescriptor, KtElement) -> KtElement>()
        val originalOffsetByExpr = LinkedHashMap<KtElement, Int>()

        val bodyOffset = body.getBlockContentOffset()
        val file = body.containingFile!!

        /*
         * Sort by descending position so that internals of value/type arguments in calls and qualified types are replaced
         * before calls/types themselves
         */
        for ((offsetInBody, resolveResult) in descriptor.extractionData.refOffsetToDeclaration.entries.sortedByDescending { it.key }) {
            val expr = file.findElementAt(bodyOffset + offsetInBody)?.getNonStrictParentOfType<KtSimpleNameExpression>()
            assert(expr != null) { "Couldn't find expression at $offsetInBody in '${body.text}'" }

            originalOffsetByExpr[expr!!] = offsetInBody

            descriptor.replacementMap[offsetInBody]?.let { replacement ->
                exprReplacementMap[expr] = replacement
            }
        }

        val replacingReturn: KtExpression?
        val expressionsToReplaceWithReturn: List<KtElement>

        val returnsForLabelRemoval = descriptor.controlFlow.outputValues
                .flatMapTo(ArrayList<KtReturnExpression>()) { it.originalExpressions.filterIsInstance<KtReturnExpression>() }

        val jumpValue = descriptor.controlFlow.jumpOutputValue
        if (jumpValue != null) {
            replacingReturn = psiFactory.createExpression(if (jumpValue.conditional) "return true" else "return")
            returnsForLabelRemoval.removeAllRaw(jumpValue.elementsToReplace)
            expressionsToReplaceWithReturn = getCounterparts(jumpValue.elementsToReplace, body, bodyOffset, file)
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

        getCounterparts(returnsForLabelRemoval, body, bodyOffset, file).forEach { it.getTargetLabel()?.delete() }

        for ((expr, originalOffset) in originalOffsetByExpr) {
            if (expr.isValid) {
                nameByOffset.put(originalOffset, exprReplacementMap[expr]?.invoke(descriptor, expr) ?: expr)
            }
        }

        if (generatorOptions.target == ExtractionTarget.PROPERTY_WITH_INITIALIZER) return

        if (body !is KtBlockExpression) throw AssertionError("Block body expected: ${descriptor.extractionData.codeFragmentText}")

        val firstExpression = body.statements.firstOrNull()
        if (firstExpression != null) {
            for (param in descriptor.parameters) {
                param.mirrorVarName?.let { varName ->
                    body.addBefore(psiFactory.createProperty(varName, null, true, param.name), firstExpression)
                    body.addBefore(psiFactory.createNewLine(), firstExpression)
                }
            }
        }

        val defaultValue = descriptor.controlFlow.defaultOutputValue

        val lastExpression = body.statements.lastOrNull()
        if (lastExpression is KtReturnExpression) return

        val (defaultExpression, expressionToUnifyWith) =
                if (!generatorOptions.inTempFile && defaultValue != null && descriptor.controlFlow.outputValueBoxer.boxingRequired && lastExpression!!.isMultiLine()) {
                    val varNameValidator = NewDeclarationNameValidator(body, lastExpression, NewDeclarationNameValidator.Target.VARIABLES)
                    val resultVal = KotlinNameSuggester.suggestNamesByType(defaultValue.valueType, varNameValidator, null).first()
                    val newDecl = body.addBefore(psiFactory.createDeclaration("val $resultVal = ${lastExpression!!.text}"), lastExpression) as KtProperty
                    body.addBefore(psiFactory.createNewLine(), lastExpression)
                    psiFactory.createExpression(resultVal) to newDecl.initializer!!
                }
                else {
                    lastExpression to null
                }

        val returnExpression = descriptor.controlFlow.outputValueBoxer.getReturnExpression(getReturnArguments(defaultExpression), psiFactory) ?: return

        when(generatorOptions.target) {
            ExtractionTarget.LAZY_PROPERTY, ExtractionTarget.FAKE_LAMBDALIKE_FUNCTION -> {
                // In the case of lazy property absence of default value means that output values are of OutputValue.Initializer type
                // We just add resulting expressions without return, since returns are prohibited in the body of lazy property
                if (defaultValue == null) {
                    body.appendElement(returnExpression.returnedExpression!!)
                }
                return
            }
        }

        when {
            defaultValue == null ->
                body.appendElement(returnExpression)

            !defaultValue.callSiteReturn ->
                replaceWithReturn(lastExpression!!, returnExpression, expressionToUnifyWith)
        }

        if (generatorOptions.allowExpressionBody) {
            val convertToExpressionBody = ConvertToExpressionBodyIntention()
            val bodyExpression = body.statements.singleOrNull()
            val bodyOwner = body.parent as KtDeclarationWithBody
            if (bodyExpression != null && !bodyExpression.isMultiLine() && convertToExpressionBody.isApplicableTo(bodyOwner)) {
                convertToExpressionBody.applyTo(bodyOwner, !descriptor.returnType.isFlexible())
            }
        }
    }

    fun insertDeclaration(declaration: KtNamedDeclaration, anchor: PsiElement): KtNamedDeclaration {
        declarationToReplace?.let { return it.replace(declaration) as KtNamedDeclaration }

        return with(descriptor.extractionData) {
            val targetContainer = anchor.parent!!
            // TODO: Get rid of explicit new-lines in favor of formatter rules
            val emptyLines = psiFactory.createWhiteSpace("\n\n")
            if (insertBefore) {
                (targetContainer.addBefore(declaration, anchor) as KtNamedDeclaration).apply {
                    targetContainer.addBefore(emptyLines, anchor)
                }
            }
            else {
                (targetContainer.addAfter(declaration, anchor) as KtNamedDeclaration).apply {
                    if (!(targetContainer is KtClassBody && (targetContainer.parent as? KtClass)?.isEnum() ?: false)) {
                        targetContainer.addAfter(emptyLines, anchor)
                    }
                }
            }
        }
    }

    val duplicates = if (generatorOptions.inTempFile) Collections.emptyList() else descriptor.duplicates

    val anchor = with(descriptor.extractionData) {
        val targetParent = targetSibling.parent

        val anchorCandidates = duplicates.mapTo(ArrayList<PsiElement>()) { it.range.elements.first() }
        anchorCandidates.add(targetSibling)
        if (targetSibling is KtEnumEntry) {
            anchorCandidates.add(targetSibling.siblings().last { it is KtEnumEntry })
        }

        val marginalCandidate = if (insertBefore) {
            anchorCandidates.minBy { it.startOffset }!!
        }
        else {
            anchorCandidates.maxBy { it.startOffset }!!
        }

        // Ascend to the level of targetSibling
        marginalCandidate.parentsWithSelf.first { it.parent == targetParent }
    }

    val shouldInsert = !(generatorOptions.inTempFile || generatorOptions.target == ExtractionTarget.FAKE_LAMBDALIKE_FUNCTION)
    val declaration = createDeclaration().let { if (shouldInsert) insertDeclaration(it, anchor) else it }
    adjustDeclarationBody(declaration)

    if (declaration is KtNamedFunction && declaration.getContainingKtFile().suppressDiagnosticsInDebugMode) {
        declaration.receiverTypeReference?.debugTypeInfo = descriptor.receiverParameter?.getParameterType(true)

        for ((i, param) in declaration.valueParameters.withIndex()) {
            param.typeReference?.debugTypeInfo = descriptor.parameters[i].getParameterType(true)
        }

        if (declaration.typeReference != null) {
            declaration.typeReference?.debugTypeInfo = descriptor.returnType.builtIns.anyType
        }
    }

    if (generatorOptions.inTempFile) return ExtractionResult(this, declaration, Collections.emptyMap(), nameByOffset)

    val replaceInitialOccurrence = {
        val arguments = descriptor.parameters.map { it.argumentText }
        makeCall(descriptor, declaration, descriptor.controlFlow, descriptor.extractionData.originalRange, arguments)
    }

    if (!generatorOptions.delayInitialOccurrenceReplacement) replaceInitialOccurrence()

    if (shouldInsert) {
        ShortenReferences.DEFAULT.process(declaration)
    }

    if (generatorOptions.inTempFile) return ExtractionResult(this, declaration, emptyMap(), nameByOffset)

    val duplicateReplacers = HashMap<KotlinPsiRange, () -> Unit>().apply {
        if (generatorOptions.delayInitialOccurrenceReplacement) {
            put(descriptor.extractionData.originalRange, replaceInitialOccurrence)
        }
        putAll(duplicates.map { it.range to { makeCall(descriptor, declaration, it.controlFlow, it.range, it.arguments) } })
    }

    if (descriptor.typeParameters.isNotEmpty()) {
        for (ref in ReferencesSearch.search(declaration, LocalSearchScope(descriptor.getOccurrenceContainer()!!))) {
            val typeArgumentList = (ref.element.parent as? KtCallExpression)?.typeArgumentList ?: continue
            if (RemoveExplicitTypeArgumentsIntention.isApplicableTo(typeArgumentList, false)) {
                typeArgumentList.delete()
            }
        }
    }

    return ExtractionResult(this, declaration, duplicateReplacers, nameByOffset)
}
