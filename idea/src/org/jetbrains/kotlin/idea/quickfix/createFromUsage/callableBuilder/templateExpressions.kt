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
import java.lang.AssertionError
import java.util.*

/**
 * Special <code>Expression</code> for parameter names based on its type.
 */
internal class ParameterNameExpression(
        private val names: Array<String>,
        private val parameterTypeToNamesMap: Map<String, Array<String>>) : Expression() {
    init {
        assert(names.all(String::isNotEmpty))
    }

    override fun calculateResult(context: ExpressionContext?): Result? {
        val lookupItems = calculateLookupItems(context) ?: return null
        return TextResult(if (lookupItems.isEmpty()) "" else lookupItems.first().lookupString)
    }

    override fun calculateQuickResult(context: ExpressionContext?) = calculateResult(context)

    override fun calculateLookupItems(context: ExpressionContext?): Array<LookupElement>? {
        context ?: return null
        val names = LinkedHashSet(this.names.toList())

        // find the parameter list
        val project = context.project ?: return null
        val offset = context.startOffset
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val editor = context.editor ?: return null
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as KtFile
        val elementAt = file.findElementAt(offset)
        val declaration = PsiTreeUtil.getParentOfType(elementAt, KtFunction::class.java, KtClass::class.java) ?: return arrayOf()
        val parameterList = when (declaration) {
            is KtFunction -> declaration.valueParameterList!!
            is KtClass -> declaration.getPrimaryConstructorParameterList()!!
            else -> throw AssertionError("Unexpected declaration: ${declaration.text}")
        }

        // add names based on selected type
        val parameter = elementAt?.getStrictParentOfType<KtParameter>()
        if (parameter != null) {
            val parameterTypeRef = parameter.typeReference
            if (parameterTypeRef != null) {
                val suggestedNamesBasedOnType = parameterTypeToNamesMap[parameterTypeRef.text]
                if (suggestedNamesBasedOnType != null) {
                    names.addAll(suggestedNamesBasedOnType)
                }
            }
        }

        // remember other parameter names for later use
        val parameterNames = parameterList.parameters.mapNotNullTo(HashSet<String>()) { jetParameter ->
            if (jetParameter == parameter) null else jetParameter.name
        }

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
internal abstract class TypeExpression(val typeCandidates: List<TypeCandidate>) : Expression() {
    class ForTypeReference(typeCandidates: List<TypeCandidate>) : TypeExpression(typeCandidates) {
        override val cachedLookupElements: Array<LookupElement> =
                typeCandidates.map { LookupElementBuilder.create(it, it.renderedTypes.first()) }.toTypedArray()
    }

    class ForDelegationSpecifier(typeCandidates: List<TypeCandidate>) : TypeExpression(typeCandidates) {
        override val cachedLookupElements: Array<LookupElement> =
                typeCandidates.map {
                    val types = it.theType.decomposeIntersection()
                    val text = (types zip it.renderedTypes).joinToString { (type, renderedType) ->
                        val descriptor = type.constructor.declarationDescriptor as ClassDescriptor
                        renderedType + if (descriptor.kind == ClassKind.INTERFACE) "" else "()"
                    }
                    LookupElementBuilder.create(it, text)
                }.toTypedArray()
    }

    protected abstract val cachedLookupElements: Array<LookupElement>

    override fun calculateResult(context: ExpressionContext?): Result {
        val lookupItems = calculateLookupItems(context)
        return TextResult(if (lookupItems.size == 0) "" else lookupItems[0].lookupString)
    }

    override fun calculateQuickResult(context: ExpressionContext?) = calculateResult(context)

    override fun calculateLookupItems(context: ExpressionContext?) = cachedLookupElements
}

/**
 * A sort-of dummy <code>Expression</code> for parameter lists, to allow us to update the parameter list as the user makes selections.
 */
internal class TypeParameterListExpression(private val mandatoryTypeParameters: List<RenderedTypeParameter>,
                                          private val parameterTypeToTypeParameterNamesMap: Map<String, List<RenderedTypeParameter>>,
                                          insertLeadingSpace: Boolean) : Expression() {
    private val prefix = if (insertLeadingSpace) " <" else "<"

    var currentTypeParameters: List<TypeParameterDescriptor> = Collections.emptyList()
        private set

    override fun calculateResult(context: ExpressionContext?): Result {
        context!!
        val project = context.project!!
        val offset = context.startOffset

        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val editor = context.editor!!
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as KtFile
        val elementAt = file.findElementAt(offset)
        val declaration = elementAt?.getStrictParentOfType<KtNamedDeclaration>() ?: return TextResult("")

        val renderedTypeParameters = LinkedHashSet<RenderedTypeParameter>()
        renderedTypeParameters.addAll(mandatoryTypeParameters)
        for (parameter in declaration.getValueParameters()) {
            val parameterTypeRef = parameter.typeReference
            if (parameterTypeRef != null) {
                val typeParameterNamesFromParameter = parameterTypeToTypeParameterNamesMap[parameterTypeRef.text]
                if (typeParameterNamesFromParameter != null) {
                    renderedTypeParameters.addAll(typeParameterNamesFromParameter)
                }
            }
        }
        val returnTypeRef = declaration.getReturnTypeReference()
        if (returnTypeRef != null) {
            val typeParameterNamesFromReturnType = parameterTypeToTypeParameterNamesMap[returnTypeRef.text]
            if (typeParameterNamesFromReturnType != null) {
                renderedTypeParameters.addAll(typeParameterNamesFromReturnType)
            }
        }


        val sortedRenderedTypeParameters = renderedTypeParameters.sortedBy { if (it.fake) it.typeParameter.index else -1 }
        currentTypeParameters = sortedRenderedTypeParameters.map { it.typeParameter }

        return TextResult(
                if (sortedRenderedTypeParameters.isEmpty()) "" else sortedRenderedTypeParameters.joinToString(", ", prefix, ">") { it.text }
        )
    }

    override fun calculateQuickResult(context: ExpressionContext?): Result = calculateResult(context)

    // do not offer the user any choices
    override fun calculateLookupItems(context: ExpressionContext?) = arrayOf<LookupElement>()
}
