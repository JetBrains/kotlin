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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.Result
import com.intellij.codeInsight.template.TextResult
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getValueParameters
import java.util.Collections
import java.util.HashSet
import java.util.LinkedHashSet

/**
 * Special <code>Expression</code> for parameter names based on its type.
 */
private class ParameterNameExpression(
        private val names: Array<String>,
        private val parameterTypeToNamesMap: Map<String, Array<String>>) : Expression() {
    init {
        assert(names all { it.isNotEmpty() })
    }

    override fun calculateResult(context: ExpressionContext?): Result? {
        val lookupItems = calculateLookupItems(context)!!
        return TextResult(if (lookupItems.isEmpty()) "" else lookupItems.first().getLookupString())
    }

    override fun calculateQuickResult(context: ExpressionContext?) = calculateResult(context)

    override fun calculateLookupItems(context: ExpressionContext?): Array<LookupElement>? {
        context!!
        val names = LinkedHashSet(this.names.toList())

        // find the parameter list
        val project = context.getProject()!!
        val offset = context.getStartOffset()
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val editor = context.getEditor()!!
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) as JetFile
        val elementAt = file.findElementAt(offset)
        val declaration = PsiTreeUtil.getParentOfType(elementAt, javaClass<JetFunction>(), javaClass<JetClass>()) ?: return arrayOf()
        val parameterList = when (declaration) {
            is JetFunction -> declaration.getValueParameterList()!!
            is JetClass -> declaration.getPrimaryConstructorParameterList()!!
            else -> throw AssertionError("Unexpected declaration: ${declaration.getText()}")
        }

        // add names based on selected type
        val parameter = elementAt?.getStrictParentOfType<JetParameter>()
        if (parameter != null) {
            val parameterTypeRef = parameter.getTypeReference()
            if (parameterTypeRef != null) {
                val suggestedNamesBasedOnType = parameterTypeToNamesMap[parameterTypeRef.getText()]
                if (suggestedNamesBasedOnType != null) {
                    names.addAll(suggestedNamesBasedOnType)
                }
            }
        }

        // remember other parameter names for later use
        val parameterNames = parameterList.getParameters().asSequence().map { jetParameter ->
            if (jetParameter == parameter) null else jetParameter.getName()
        }.filterNotNullTo(HashSet<String>())

        // add fallback parameter name
        if (names.isEmpty()) {
            names.add("arg")
        }

        // ensure there are no conflicts
        val validator = CollectingNameValidator(parameterNames)
        return names.map { LookupElementBuilder.create(KotlinNameSuggester.suggestNameByName(it, validator)) }.toTypedArray()
    }
}

/**
 * An <code>Expression</code> for type references and delegation specifiers.
 */
private abstract class TypeExpression(public val typeCandidates: List<TypeCandidate>) : Expression() {
    class ForTypeReference(typeCandidates: List<TypeCandidate>) : TypeExpression(typeCandidates) {
        override val cachedLookupElements: Array<LookupElement> =
                typeCandidates.map { LookupElementBuilder.create(it, it.renderedType!!) }.toTypedArray()
    }

    class ForDelegationSpecifier(typeCandidates: List<TypeCandidate>) : TypeExpression(typeCandidates) {
        override val cachedLookupElements: Array<LookupElement> =
                typeCandidates.map {
                    val descriptor = it.theType.getConstructor().getDeclarationDescriptor() as ClassDescriptor
                    val text = it.renderedType!! + if (descriptor.getKind() == ClassKind.INTERFACE) "" else "()"
                    LookupElementBuilder.create(it, text)
                }.toTypedArray()
    }

    protected abstract val cachedLookupElements: Array<LookupElement>

    override fun calculateResult(context: ExpressionContext?): Result {
        val lookupItems = calculateLookupItems(context)
        return TextResult(if (lookupItems.size() == 0) "" else lookupItems[0].getLookupString())
    }

    override fun calculateQuickResult(context: ExpressionContext?) = calculateResult(context)

    override fun calculateLookupItems(context: ExpressionContext?) = cachedLookupElements
}

/**
 * A sort-of dummy <code>Expression</code> for parameter lists, to allow us to update the parameter list as the user makes selections.
 */
private class TypeParameterListExpression(private val mandatoryTypeParameters: List<RenderedTypeParameter>,
                                          private val parameterTypeToTypeParameterNamesMap: Map<String, List<RenderedTypeParameter>>,
                                          insertLeadingSpace: Boolean) : Expression() {
    private val prefix = if (insertLeadingSpace) " <" else "<"

    public var currentTypeParameters: List<TypeParameterDescriptor> = Collections.emptyList()
        private set

    override fun calculateResult(context: ExpressionContext?): Result {
        context!!
        val project = context.getProject()!!
        val offset = context.getStartOffset()

        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val editor = context.getEditor()!!
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) as JetFile
        val elementAt = file.findElementAt(offset)
        val declaration = elementAt?.getStrictParentOfType<JetNamedDeclaration>() ?: return TextResult("")

        val renderedTypeParameters = LinkedHashSet<RenderedTypeParameter>()
        renderedTypeParameters.addAll(mandatoryTypeParameters)
        for (parameter in declaration.getValueParameters()) {
            val parameterTypeRef = parameter.getTypeReference()
            if (parameterTypeRef != null) {
                val typeParameterNamesFromParameter = parameterTypeToTypeParameterNamesMap[parameterTypeRef.getText()]
                if (typeParameterNamesFromParameter != null) {
                    renderedTypeParameters.addAll(typeParameterNamesFromParameter)
                }
            }
        }
        val returnTypeRef = declaration.getReturnTypeReference()
        if (returnTypeRef != null) {
            val typeParameterNamesFromReturnType = parameterTypeToTypeParameterNamesMap[returnTypeRef.getText()]
            if (typeParameterNamesFromReturnType != null) {
                renderedTypeParameters.addAll(typeParameterNamesFromReturnType)
            }
        }


        val sortedRenderedTypeParameters = renderedTypeParameters.sortBy { if (it.fake) it.typeParameter.getIndex() else -1}
        currentTypeParameters = sortedRenderedTypeParameters.map { it.typeParameter }

        return TextResult(
                if (sortedRenderedTypeParameters.isEmpty()) "" else sortedRenderedTypeParameters.map { it.text }.joinToString(", ", prefix, ">")
        )
    }

    override fun calculateQuickResult(context: ExpressionContext?): Result = calculateResult(context)

    // do not offer the user any choices
    override fun calculateLookupItems(context: ExpressionContext?) = arrayOf<LookupElement>()
}

private object ValVarExpression: Expression() {
    private val cachedLookupElements = listOf("val", "var").map { LookupElementBuilder.create(it) }.toTypedArray<LookupElement>()

    override fun calculateResult(context: ExpressionContext?): Result? = TextResult("val")

    override fun calculateQuickResult(context: ExpressionContext?): Result? = calculateResult(context)

    override fun calculateLookupItems(context: ExpressionContext?): Array<LookupElement>? = cachedLookupElements
}
