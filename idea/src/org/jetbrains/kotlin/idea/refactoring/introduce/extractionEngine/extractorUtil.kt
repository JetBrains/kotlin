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
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetPsiFactory.CallableBuilder
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.JetPsiFactory
import java.util.LinkedHashMap
import java.util.Collections
import org.jetbrains.kotlin.psi.psiUtil.isFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.psi.JetFunctionLiteralArgument
import org.jetbrains.kotlin.idea.util.psiModificationUtil.moveInsideParenthesesAndReplaceWith
import org.jetbrains.kotlin.psi.psiUtil.appendElement
import org.jetbrains.kotlin.psi.psiUtil.replaced
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.ParameterUpdate
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.Jump
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.Initializer
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.ExpressionValue
import org.jetbrains.kotlin.psi.JetReturnExpression
import org.jetbrains.kotlin.idea.refactoring.JetNameValidatorImpl
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.refactoring.JetNameSuggester
import org.jetbrains.kotlin.idea.refactoring.JetNameValidatorImpl
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.*
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.refactoring.isMultiLine
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValueBoxer.AsTuple
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiRange
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiRange.Match
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.UnificationResult.StronglyMatched
import org.jetbrains.kotlin.idea.util.psi.patternMatching.UnificationResult.WeaklyMatched
import org.jetbrains.kotlin.idea.util.psi.patternMatching.UnifierParameter
import org.jetbrains.kotlin.idea.util.psiModificationUtil.moveInsideParenthesesAndReplaceWith
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.JetPsiFactory.CallableBuilder
import org.jetbrains.kotlin.psi.JetPsiFactory.CallableBuilder.Target
import org.jetbrains.kotlin.psi.codeFragmentUtil.DEBUG_TYPE_REFERENCE_STRING
import org.jetbrains.kotlin.psi.codeFragmentUtil.debugTypeInfo
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.JetType
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.LinkedHashMap

fun ExtractionGeneratorConfiguration.getDeclarationText(
        withBody: Boolean = true,
        descriptorRenderer: DescriptorRenderer = if (generatorOptions.flexibleTypesAllowed)
                                                    DescriptorRenderer.FLEXIBLE_TYPES_FOR_CODE
                                                 else IdeDescriptorRenderers.SOURCE_CODE
): String {
    val extractionTarget = generatorOptions.target
    if (!extractionTarget.isAvailable(descriptor)) {
        throw IllegalArgumentException("Can't generate ${extractionTarget.name}: ${descriptor.extractionData.codeFragmentText}")
    }

    val builderTarget = if (extractionTarget == ExtractionTarget.FUNCTION) CallableBuilder.Target.FUNCTION else CallableBuilder.Target.READ_ONLY_PROPERTY
    return CallableBuilder(builderTarget).let { builder ->
        builder.modifier(descriptor.visibility)

        builder.typeParams(descriptor.typeParameters.map { it.originalDeclaration.getText()!! })

        fun JetType.typeAsString(): String {
            return if (isSpecial()) DEBUG_TYPE_REFERENCE_STRING else descriptorRenderer.renderType(this)
        }

        descriptor.receiverParameter?.let {
            builder.receiver(it.getParameterType(descriptor.extractionData.options.allowSpecialClassNames).typeAsString())
        }

        builder.name(if (descriptor.name != "") descriptor.name else DEFAULT_FUNCTION_NAME)

        descriptor.parameters.forEach { parameter ->
            builder.param(parameter.name,
                          parameter.getParameterType(descriptor.extractionData.options.allowSpecialClassNames).typeAsString())
        }

        with(descriptor.controlFlow.outputValueBoxer.returnType) {
            if (isDefault() || isError() || extractionTarget == ExtractionTarget.PROPERTY_WITH_INITIALIZER) {
                builder.noReturnType()
            } else {
                builder.returnType(typeAsString())
            }
        }

        builder.typeConstraints(descriptor.typeParameters.flatMap { it.originalConstraints }.map { it.getText()!! })

        if (withBody) {
            val bodyText = descriptor.extractionData.codeFragmentText
            when (extractionTarget) {
                ExtractionTarget.FUNCTION, ExtractionTarget.PROPERTY_WITH_GETTER -> builder.blockBody(bodyText)
                ExtractionTarget.PROPERTY_WITH_INITIALIZER -> builder.initializer(bodyText)
                ExtractionTarget.LAZY_PROPERTY -> builder.lazyBody(bodyText)
            }
        }

        builder.asString()
    }
}

fun JetType.isSpecial(): Boolean {
    return this.getConstructor().getDeclarationDescriptor()?.getName()?.isSpecial() ?: false
}

fun createNameCounterpartMap(from: JetElement, to: JetElement): Map<JetSimpleNameExpression, JetSimpleNameExpression> {
    val map = HashMap<JetSimpleNameExpression, JetSimpleNameExpression>()

    val fromOffset = from.getTextRange()!!.getStartOffset()
    from.accept(
            object: JetTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                    val offset = expression.getTextRange()!!.getStartOffset() - fromOffset
                    val newExpression = to.findElementAt(offset)?.getNonStrictParentOfType<JetSimpleNameExpression>()
                    assert(newExpression!= null, "Couldn't find expression at $offset in '${to.getText()}'")

                    map[expression] = newExpression!!
                }
            }
    )

    return map
}

class DuplicateInfo(
        val range: JetPsiRange,
        val controlFlow: ControlFlow,
        val arguments: List<String>
)

fun ExtractableCodeDescriptor.findDuplicates(): List<DuplicateInfo> {
    fun processWeakMatch(match: Match, newControlFlow: ControlFlow): Boolean {
        val valueCount = controlFlow.outputValues.size()

        val weakMatches = HashMap((match.result as WeaklyMatched).weakMatches)
        val currentValuesToNew = HashMap<OutputValue, OutputValue>()

        fun matchValues(currentValue: OutputValue, newValue: OutputValue): Boolean {
            if ((currentValue is Jump) != (newValue is Jump)) return false
            if (currentValue.originalExpressions.zip(newValue.originalExpressions).all { weakMatches[it.first] == it.second }) {
                currentValuesToNew[currentValue] = newValue
                weakMatches.keySet().removeAll(currentValue.originalExpressions)
                return true
            }
            return false
        }

        if (valueCount == 1) {
            matchValues(controlFlow.outputValues.first(), newControlFlow.outputValues.first())
        } else {
            @outer
            for (currentValue in controlFlow.outputValues)
                for (newValue in newControlFlow.outputValues) {
                    if ((currentValue is ExpressionValue) != (newValue is ExpressionValue)) continue
                    if (matchValues(currentValue, newValue)) continue @outer
                }
        }

        return currentValuesToNew.size() == valueCount && weakMatches.isEmpty()
    }

    fun getControlFlowIfMatched(match: Match): ControlFlow? {
        val analysisResult = extractionData.copy(originalRange = match.range).performAnalysis()
        if (analysisResult.status != AnalysisResult.Status.SUCCESS) return null

        val newControlFlow = analysisResult.descriptor!!.controlFlow
        if (newControlFlow.outputValues.isEmpty()) return newControlFlow
        if (controlFlow.outputValues.size() != newControlFlow.outputValues.size()) return null

        val matched = when (match.result) {
            is StronglyMatched -> true
            is WeaklyMatched -> processWeakMatch(match, newControlFlow)
            else -> throw AssertionError("Unexpected unification result: ${match.result}")
        }

        return if (matched) newControlFlow else null
    }

    val unifierParameters = parameters.map { UnifierParameter(it.originalDescriptor, it.getParameterType(extractionData.options.allowSpecialClassNames)) }

    val unifier = JetPsiUnifier(unifierParameters, true)

    val scopeElement = extractionData.targetSibling.getParent() ?: return Collections.emptyList()
    val originalTextRange = extractionData.originalRange.getTextRange()
    return extractionData
            .originalRange
            .match(scopeElement, unifier)
            .stream()
            .filter { !(it.range.getTextRange() intersects originalTextRange) }
            .map { match ->
                val controlFlow = getControlFlowIfMatched(match)
                controlFlow?.let { DuplicateInfo(match.range, it, unifierParameters.map { match.result.substitution[it]!!.getText()!! }) }
            }
            .filterNotNull()
            .toList()
}

private fun makeCall(
        extractableDescriptor: ExtractableCodeDescriptor,
        declaration: JetNamedDeclaration,
        controlFlow: ControlFlow,
        rangeToReplace: JetPsiRange,
        arguments: List<String>) {
    fun insertCall(anchor: PsiElement, wrappedCall: JetExpression) {
        val firstExpression = rangeToReplace.elements.firstOrNull { it is JetExpression } as? JetExpression
        if (firstExpression?.isFunctionLiteralOutsideParentheses() ?: false) {
            val functionLiteralArgument = firstExpression?.getStrictParentOfType<JetFunctionLiteralArgument>()!!
            functionLiteralArgument.moveInsideParenthesesAndReplaceWith(wrappedCall, extractableDescriptor.originalContext)
            return
        }
        anchor.replace(wrappedCall)
    }

    if (rangeToReplace !is JetPsiRange.ListRange) return

    val anchor = rangeToReplace.startElement
    val anchorParent = anchor.getParent()!!

    anchor.getNextSibling()?.let { from ->
        val to = rangeToReplace.endElement
        if (to != anchor) {
            anchorParent.deleteChildRange(from, to);
        }
    }

    val calleeName = declaration.getName()
    val callText = when (declaration) {
        is JetNamedFunction ->
            arguments.joinToString(separator = ", ", prefix = "$calleeName(", postfix = ")")
        else -> calleeName
    }

    val anchorInBlock = stream(anchor) { it.getParent() }.firstOrNull { it.getParent() is JetBlockExpression }
    val block = (anchorInBlock?.getParent() as? JetBlockExpression) ?: anchorParent

    val psiFactory = JetPsiFactory(anchor.getProject())
    val newLine = psiFactory.createNewLine()

    if (controlFlow.outputValueBoxer is AsTuple && controlFlow.outputValues.size() > 1 && controlFlow.outputValues.all { it is Initializer }) {
        val declarationsToMerge = controlFlow.outputValues.map { (it as Initializer).initializedDeclaration }
        val isVar = declarationsToMerge.first().isVar()
        if (declarationsToMerge.all { it.isVar() == isVar }) {
            controlFlow.declarationsToCopy.subtract(declarationsToMerge).forEach {
                block.addBefore(psiFactory.createDeclaration<JetDeclaration>(it.getText()!!), anchorInBlock) as JetDeclaration
                block.addBefore(newLine, anchorInBlock)
            }

            val entries = declarationsToMerge.map { p -> p.getName() + (p.getTypeReference()?.let { ": ${it.getText()}" } ?: "") }
            anchorInBlock?.replace(
                    psiFactory.createDeclaration("${if (isVar) "var" else "val"} (${entries.joinToString()}) = $callText")
            )

            return
        }
    }

    val inlinableCall = controlFlow.outputValues.size() <= 1
    val unboxingExpressions =
            if (inlinableCall) {
                controlFlow.outputValueBoxer.getUnboxingExpressions(callText)
            }
            else {
                val varNameValidator = JetNameValidatorImpl(block, anchorInBlock, JetNameValidatorImpl.Target.PROPERTIES)
                val resultVal = JetNameSuggester.suggestNames(controlFlow.outputValueBoxer.returnType, varNameValidator, null).first()
                block.addBefore(psiFactory.createDeclaration("val $resultVal = $callText"), anchorInBlock)
                block.addBefore(newLine, anchorInBlock)
                controlFlow.outputValueBoxer.getUnboxingExpressions(resultVal)
            }

    val copiedDeclarations = HashMap<JetDeclaration, JetDeclaration>()
    for (decl in controlFlow.declarationsToCopy) {
        val declCopy = psiFactory.createDeclaration<JetDeclaration>(decl.getText()!!)
        copiedDeclarations[decl] = block.addBefore(declCopy, anchorInBlock) as JetDeclaration
        block.addBefore(newLine, anchorInBlock)
    }

    if (controlFlow.outputValues.isEmpty()) {
        anchor.replace(psiFactory.createExpression(callText))
        return
    }

    fun wrapCall(outputValue: OutputValue, callText: String): List<PsiElement> {
        return when (outputValue) {
            is OutputValue.ExpressionValue ->
                Collections.singletonList(
                        if (outputValue.callSiteReturn) psiFactory.createReturn(callText) else psiFactory.createExpression(callText)
                )

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
                            psiFactory.createExpression("if ($callText) ${outputValue.elementToInsertAfterCall.getText()}")
                    )
                }
                else {
                    listOf(
                            psiFactory.createExpression(callText),
                            newLine,
                            psiFactory.createExpression(outputValue.elementToInsertAfterCall.getText()!!)
                    )
                }
            }

            is Initializer -> {
                val newProperty = copiedDeclarations[outputValue.initializedDeclaration] as JetProperty
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
        insertCall(anchor, wrapCall(it, unboxingExpressions[it]!!).first() as JetExpression)
    }

    if (anchor.isValid()) {
        anchor.delete()
    }
}

fun ExtractionGeneratorConfiguration.generateDeclaration(
        declarationToReplace: JetNamedDeclaration? = null
): ExtractionResult{
    val psiFactory = JetPsiFactory(descriptor.extractionData.originalFile)
    val nameByOffset = HashMap<Int, JetElement>()

    fun createDeclaration(): JetNamedDeclaration {
        return with(descriptor.extractionData) {
            if (generatorOptions.inTempFile) {
                createTemporaryDeclaration("${getDeclarationText()}\n")
            }
            else {
                psiFactory.createDeclaration(getDeclarationText())
            }
        }
    }

    fun getReturnArguments(resultExpression: JetExpression?): List<String> {
        return descriptor.controlFlow.outputValues
                .map {
                    when (it) {
                        is ExpressionValue -> resultExpression?.getText()
                        is Jump -> if (it.conditional) "false" else null
                        is ParameterUpdate -> it.parameter.nameForRef
                        is Initializer -> it.initializedDeclaration.getName()
                        else -> throw IllegalArgumentException("Unknown output value: $it")
                    }
                }
                .filterNotNull()
    }

    fun replaceWithReturn(
            originalExpression: JetExpression,
            replacingExpression: JetReturnExpression,
            expressionToUnifyWith: JetExpression?
    ) {
        val currentResultExpression =
                if (originalExpression is JetReturnExpression) originalExpression.getReturnedExpression() else originalExpression
        if (currentResultExpression == null) return

        val newResultExpression = descriptor.controlFlow.defaultOutputValue?.let {
            val boxedExpression = originalExpression.replaced(replacingExpression).getReturnedExpression()!!
            descriptor.controlFlow.outputValueBoxer.extractExpressionByValue(boxedExpression, it)
        }
        if (newResultExpression == null) {
            throw AssertionError("Can' replace '${originalExpression.getText()}' with '${replacingExpression.getText()}'")
        }

        val counterpartMap = createNameCounterpartMap(currentResultExpression, expressionToUnifyWith ?: newResultExpression)
        nameByOffset.entrySet().forEach { e -> counterpartMap[e.getValue()]?.let { e.setValue(it) } }
    }

    fun adjustDeclarationBody(declaration: JetNamedDeclaration) {
        val body = declaration.getGeneratedBody()

        val exprReplacementMap = HashMap<JetElement, (JetElement) -> JetElement>()
        val originalOffsetByExpr = LinkedHashMap<JetElement, Int>()

        val bodyOffset = body.getBlockContentOffset()
        val file = body.getContainingFile()!!

        /*
         * Sort by descending position so that internals of value/type arguments in calls and qualified types are replaced
         * before calls/types themselves
         */
        for ((offsetInBody, resolveResult) in descriptor.extractionData.refOffsetToDeclaration.entrySet().sortDescendingBy { it.key }) {
            val expr = file.findElementAt(bodyOffset + offsetInBody)?.getNonStrictParentOfType<JetSimpleNameExpression>()
            assert(expr != null, "Couldn't find expression at $offsetInBody in '${body.getText()}'")

            originalOffsetByExpr[expr!!] = offsetInBody

            descriptor.replacementMap[offsetInBody]?.let { replacement ->
                if (replacement !is ParameterReplacement || replacement.parameter != descriptor.receiverParameter) {
                    exprReplacementMap[expr] = replacement
                }
            }
        }

        val replacingReturn: JetExpression?
        val expressionsToReplaceWithReturn: List<JetElement>

        val jumpValue = descriptor.controlFlow.jumpOutputValue
        if (jumpValue != null) {
            replacingReturn = psiFactory.createExpression(if (jumpValue.conditional) "return true" else "return")
            expressionsToReplaceWithReturn = jumpValue.elementsToReplace.map { jumpElement ->
                val offsetInBody = jumpElement.getTextRange()!!.getStartOffset() - descriptor.extractionData.originalStartOffset!!
                val expr = file.findElementAt(bodyOffset + offsetInBody)?.getNonStrictParentOfType(jumpElement.javaClass)
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

        if (generatorOptions.target == ExtractionTarget.PROPERTY_WITH_INITIALIZER) return

        if (body !is JetBlockExpression) throw AssertionError("Block body expected: ${descriptor.extractionData.codeFragmentText}")

        val firstExpression = body.getStatements().firstOrNull()
        if (firstExpression != null) {
            for (param in descriptor.parameters) {
                param.mirrorVarName?.let { varName ->
                    body.addBefore(psiFactory.createProperty(varName, null, true, param.name), firstExpression)
                    body.addBefore(psiFactory.createNewLine(), firstExpression)
                }
            }
        }

        val defaultValue = descriptor.controlFlow.defaultOutputValue

        val lastExpression = body.getStatements().lastOrNull() as? JetExpression
        if (lastExpression is JetReturnExpression) return

        val (defaultExpression, expressionToUnifyWith) =
                if (!generatorOptions.inTempFile && defaultValue != null && descriptor.controlFlow.outputValueBoxer.boxingRequired && lastExpression!!.isMultiLine()) {
                    val varNameValidator = JetNameValidatorImpl(body, lastExpression, JetNameValidatorImpl.Target.PROPERTIES)
                    val resultVal = JetNameSuggester.suggestNames(defaultValue.valueType, varNameValidator, null).first()
                    val newDecl = body.addBefore(psiFactory.createDeclaration("val $resultVal = ${lastExpression!!.getText()}"), lastExpression) as JetProperty
                    body.addBefore(psiFactory.createNewLine(), lastExpression)
                    psiFactory.createExpression(resultVal) to newDecl.getInitializer()!!
                }
                else {
                    lastExpression to null
                }

        val returnExpression = descriptor.controlFlow.outputValueBoxer.getReturnExpression(getReturnArguments(defaultExpression), psiFactory)
        if (returnExpression == null) return

        if (generatorOptions.target == ExtractionTarget.LAZY_PROPERTY) {
            // In the case of lazy property absence of default value means that output values are of OutputValue.Initializer type
            // We just add resulting expressions without return, since returns are prohibited in the body of lazy property
            if (defaultValue == null) {
                body.appendElement(returnExpression.getReturnedExpression()!!)
            }
            return
        }

        when {
            defaultValue == null ->
                body.appendElement(returnExpression)

            !defaultValue.callSiteReturn ->
                replaceWithReturn(lastExpression!!, returnExpression, expressionToUnifyWith)
        }
    }

    fun insertDeclaration(declaration: JetNamedDeclaration, anchor: PsiElement): JetNamedDeclaration {
        declarationToReplace?.let { return it.replace(declaration) as JetNamedDeclaration }

        return with(descriptor.extractionData) {
            val targetContainer = anchor.getParent()!!
            val emptyLines = psiFactory.createWhiteSpace("\n\n")
            if (insertBefore) {
                val declarationInFile = targetContainer.addBefore(declaration, anchor) as JetNamedDeclaration
                targetContainer.addBefore(emptyLines, anchor)

                declarationInFile
            }
            else {
                val declarationInFile = targetContainer.addAfter(declaration, anchor) as JetNamedDeclaration
                targetContainer.addAfter(emptyLines, anchor)

                declarationInFile
            }
        }
    }

    val duplicates = if (generatorOptions.inTempFile) Collections.emptyList() else descriptor.duplicates

    val anchor = with(descriptor.extractionData) {
        val anchorCandidates = duplicates.mapTo(ArrayList<PsiElement>()) { it.range.elements.first() }
        anchorCandidates.add(targetSibling)

        val marginalCandidate = if (insertBefore) {
            anchorCandidates.minBy { it.getTextRange().getStartOffset() }!!
        }
        else {
            anchorCandidates.maxBy { it.getTextRange().getStartOffset() }!!
        }

        // Ascend to the level of targetSibling
        val targetParent = targetSibling.getParent()
        marginalCandidate.parents().first { it.getParent() == targetParent }
    }

    val declaration = createDeclaration().let { if (generatorOptions.inTempFile) it else insertDeclaration(it, anchor) }
    adjustDeclarationBody(declaration)

    if (declaration is JetNamedFunction && declaration.getContainingJetFile().suppressDiagnosticsInDebugMode) {
        declaration.getReceiverTypeReference()?.debugTypeInfo = descriptor.receiverParameter?.getParameterType(true)

        for ((i, param) in declaration.getValueParameters().withIndex()) {
            param.getTypeReference()?.debugTypeInfo = descriptor.parameters[i].getParameterType(true)
        }

        if (declaration.getTypeReference() != null) {
            declaration.getTypeReference()?.debugTypeInfo = KotlinBuiltIns.getInstance().getAnyType()
        }
    }

    if (generatorOptions.inTempFile) return ExtractionResult(this, declaration, Collections.emptyMap(), nameByOffset)

    makeCall(descriptor, declaration, descriptor.controlFlow, descriptor.extractionData.originalRange, descriptor.parameters.map { it.argumentText })
    ShortenReferences.DEFAULT.process(declaration)

    val duplicateReplacers = duplicates.map { it.range to { makeCall(descriptor, declaration, it.controlFlow, it.range, it.arguments) } }.toMap()
    return ExtractionResult(this, declaration, duplicateReplacers, nameByOffset)
}
