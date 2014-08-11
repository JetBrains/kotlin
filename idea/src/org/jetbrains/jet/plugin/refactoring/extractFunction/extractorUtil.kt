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

import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import java.util.HashMap
import org.jetbrains.jet.lang.psi.JetTreeVisitorVoid
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetPsiFactory.CallableBuilder
import org.jetbrains.jet.lang.psi.JetPsiFactory.CallableBuilder.Target
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.lang.psi.JetPsiFactory
import java.util.LinkedHashMap
import java.util.Collections
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import org.jetbrains.jet.lang.psi.psiUtil.isFunctionLiteralOutsideParentheses
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetFunctionLiteralArgument
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.util.psiModificationUtil.moveInsideParenthesesAndReplaceWith
import org.jetbrains.jet.lang.psi.psiUtil.prependElement
import org.jetbrains.jet.lang.psi.psiUtil.appendElement
import org.jetbrains.jet.lang.psi.psiUtil.replaced
import org.jetbrains.jet.plugin.intentions.declarations.DeclarationUtils

fun ExtractableCodeDescriptor.getDeclarationText(
        options: ExtractionGeneratorOptions = ExtractionGeneratorOptions.DEFAULT,
        withBody: Boolean = true,
        descriptorRenderer: DescriptorRenderer = DescriptorRenderer.FQ_NAMES_IN_TYPES
): String {
    if (!canGenerateProperty() && options.extractAsProperty) {
        throw IllegalArgumentException("Can't generate property: ${extractionData.getCodeFragmentText()}")
    }

    val builderTarget = if (options.extractAsProperty) Target.READ_ONLY_PROPERTY else Target.FUNCTION
    return CallableBuilder(builderTarget).let { builder ->
        builder.modifier(visibility)

        builder.typeParams(typeParameters.map { it.originalDeclaration.getText()!! })

        receiverParameter?.let { builder.receiver(descriptorRenderer.renderType(it.parameterType)) }

        builder.name(name)

        parameters.forEach { parameter ->
            builder.param(parameter.name, descriptorRenderer.renderType(parameter.parameterType))
        }

        with(controlFlow.returnType) {
            if (isDefault() || isError()) builder.noReturnType() else builder.returnType(descriptorRenderer.renderType(this))
        }

        builder.typeConstraints(typeParameters.flatMap { it.originalConstraints }.map { it.getText()!! })

        if (withBody) {
            builder.blockBody(extractionData.getCodeFragmentText())
        }

        builder.asString()
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

fun ExtractableCodeDescriptor.generateDeclaration(options: ExtractionGeneratorOptions): ExtractionResult{
    val psiFactory = JetPsiFactory(extractionData.originalFile)
    val nameByOffset = HashMap<Int, JetElement>()

    fun createDeclaration(): JetNamedDeclaration {
        return with(extractionData) {
            if (options.inTempFile) {
                createTemporaryDeclaration("${getDeclarationText()}\n")
            }
            else {
                psiFactory.createDeclaration(getDeclarationText(options))
            }
        }
    }

    fun adjustDeclarationBody(declaration: JetNamedDeclaration) {
        val body = declaration.getGeneratedBlockBody()

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
            replacingReturn = psiFactory.createExpression(if (controlFlow is ConditionalJump) "return true" else "return")
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
                body.prependElement(psiFactory.createProperty(varName, null, true, param.name))
            }
        }

        when (controlFlow) {
            is ParameterUpdate ->
                body.appendElement(psiFactory.createReturn(controlFlow.parameter.nameForRef))

            is Initializer ->
                body.appendElement(psiFactory.createReturn(controlFlow.initializedDeclaration.getName()!!))

            is ConditionalJump ->
                body.appendElement(psiFactory.createReturn("false"))

            is ExpressionEvaluation ->
                body.getStatements().last?.let {
                    val newExpr = it.replaced(
                            psiFactory.createReturn(
                                    it.getText() ?: throw AssertionError("Return expression shouldn't be empty: code fragment = ${body.getText()}")
                            )
                    ).getReturnedExpression()!!
                    val counterpartMap = createNameCounterpartMap(it, newExpr)
                    nameByOffset.entrySet().forEach { e -> counterpartMap[e.getValue()]?.let { e.setValue(it) } }
                }
        }
    }

    fun insertDeclaration(declaration: JetNamedDeclaration): JetNamedDeclaration {
        return with(extractionData) {
            val targetContainer = targetSibling.getParent()!!
            val emptyLines = psiFactory.createWhiteSpace("\n\n")
            if (insertBefore) {
                val declarationInFile = targetContainer.addBefore(declaration, targetSibling) as JetNamedDeclaration
                targetContainer.addBefore(emptyLines, targetSibling)

                declarationInFile
            }
            else {
                val declarationInFile = targetContainer.addAfter(declaration, targetSibling) as JetNamedDeclaration
                targetContainer.addAfter(emptyLines, targetSibling)

                declarationInFile
            }
        }
    }

    fun insertCall(anchor: PsiElement, wrappedCall: JetExpression?) {
        if (wrappedCall == null) {
            anchor.delete()
            return
        }

        val firstExpression = extractionData.getExpressions().firstOrNull()
        if (firstExpression?.isFunctionLiteralOutsideParentheses() ?: false) {
            val functionLiteralArgument = PsiTreeUtil.getParentOfType(firstExpression, javaClass<JetFunctionLiteralArgument>())!!
            //todo use the right binding context
            functionLiteralArgument.moveInsideParenthesesAndReplaceWith(wrappedCall, BindingContext.EMPTY)
            return
        }
        anchor.replace(wrappedCall)
    }

    fun makeCall(declaration: JetNamedDeclaration): JetNamedDeclaration {
        val anchor = extractionData.originalElements.first
        if (anchor == null) return declaration

        val anchorParent = anchor.getParent()!!

        anchor.getNextSibling()?.let { from ->
            val to = extractionData.originalElements.last
            if (to != anchor) {
                anchorParent.deleteChildRange(from, to);
            }
        }

        val callText = when (declaration) {
            is JetNamedFunction ->
                parameters
                        .map { it.argumentText }
                        .joinToString(separator = ", ", prefix = "${name}(", postfix = ")")
            else -> name
        }

        val copiedDeclarations = HashMap<JetDeclaration, JetDeclaration>()
        for (decl in controlFlow.declarationsToCopy) {
            val declCopy = psiFactory.createDeclaration<JetDeclaration>(decl.getText()!!)
            copiedDeclarations[decl] = anchorParent.addBefore(declCopy, anchor) as JetDeclaration
            anchorParent.addBefore(psiFactory.createNewLine(), anchor)
        }

        val wrappedCall = when (controlFlow) {
            is ExpressionEvaluationWithCallSiteReturn ->
                psiFactory.createReturn(callText)

            is ParameterUpdate ->
                psiFactory.createExpression("${controlFlow.parameter.argumentText} = $callText")

            is Initializer -> {
                val newDecl = copiedDeclarations[controlFlow.initializedDeclaration] as JetProperty
                newDecl.replace(DeclarationUtils.changePropertyInitializer(newDecl, psiFactory.createExpression(callText)))
                null
            }

            is ConditionalJump ->
                psiFactory.createExpression("if ($callText) ${controlFlow.elementToInsertAfterCall.getText()}")

            is UnconditionalJump -> {
                anchorParent.addAfter(
                        psiFactory.createExpression(controlFlow.elementToInsertAfterCall.getText()!!),
                        anchor
                )
                anchorParent.addAfter(psiFactory.createNewLine(), anchor)

                psiFactory.createExpression(callText)
            }

            else ->
                psiFactory.createExpression(callText)
        }
        insertCall(anchor, wrappedCall)

        return declaration
    }

    val declaration = createDeclaration()
    adjustDeclarationBody(declaration)

    if (options.inTempFile) return ExtractionResult(declaration, nameByOffset)

    val declarationInPlace = makeCall(insertDeclaration(declaration))
    ShortenReferences.process(declarationInPlace)
    return ExtractionResult(declaration, nameByOffset)
}