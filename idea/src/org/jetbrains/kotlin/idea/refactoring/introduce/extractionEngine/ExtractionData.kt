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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.JetExpression
import com.intellij.openapi.util.TextRange
import kotlin.properties.Delegates
import java.util.HashMap
import org.jetbrains.kotlin.idea.codeInsight.JetFileReferencesResolver
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToDeclarationUtil
import java.util.Collections
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.isInsideOf
import java.util.ArrayList
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.psi.JetSuperExpression
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetDeclarationWithBody
import org.jetbrains.kotlin.psi.JetUserType
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetFunctionLiteral
import org.jetbrains.kotlin.psi.JetClassInitializer
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiRange
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.idea.refactoring.compareDescriptors

data class ExtractionOptions(
        val inferUnitTypeForUnusedValues: Boolean = true,
        val enableListBoxing: Boolean = false,
        val extractAsProperty: Boolean = false,
        val allowSpecialClassNames: Boolean = false
) {
    class object {
        val DEFAULT = ExtractionOptions()
    }
}

data class ResolveResult(
        val originalRefExpr: JetSimpleNameExpression,
        val declaration: PsiNamedElement,
        val descriptor: DeclarationDescriptor,
        val resolvedCall: ResolvedCall<*>?
)

data class ResolvedReferenceInfo(
        val refExpr: JetSimpleNameExpression,
        val offsetInBody: Int,
        val resolveResult: ResolveResult
)

data class ExtractionData(
        val originalFile: JetFile,
        val originalRange: JetPsiRange,
        val targetSibling: PsiElement,
        val options: ExtractionOptions = ExtractionOptions.DEFAULT
) {
    val project: Project = originalFile.getProject()
    val originalElements: List<PsiElement> = originalRange.elements

    val insertBefore: Boolean = options.extractAsProperty
                                || targetSibling.getStrictParentOfType<JetDeclaration>()?.let {
                                    it is JetDeclarationWithBody || it is JetClassInitializer
                                } ?: false

    fun getExpressions(): List<JetExpression> = originalElements.filterIsInstance<JetExpression>()

    private fun getCodeFragmentTextRange(): TextRange? {
        val originalElements = originalElements
        return when (originalElements.size()) {
            0 -> null
            1 -> originalElements.first().getTextRange()
            else -> {
                val from = originalElements.first().getTextRange()!!.getStartOffset()
                val to = originalElements.last().getTextRange()!!.getEndOffset()
                TextRange(from, to)
            }
        }
    }

    val codeFragmentText: String by Delegates.lazy {
        getCodeFragmentTextRange()?.let { originalFile.getText()?.substring(it.getStartOffset(), it.getEndOffset()) } ?: ""
    }

    val originalStartOffset = originalElements.firstOrNull()?.let { e -> e.getTextRange()!!.getStartOffset() }

    private val itFakeDeclaration by Delegates.lazy { JetPsiFactory(originalFile).createParameter("it: Any?") }

    val refOffsetToDeclaration by Delegates.lazy {
        fun isExtractableIt(descriptor: DeclarationDescriptor, context: BindingContext): Boolean {
            if (!(descriptor is ValueParameterDescriptor && (context[BindingContext.AUTO_CREATED_IT, descriptor] ?: false))) return false
            val function = DescriptorToSourceUtils.descriptorToDeclaration(descriptor.getContainingDeclaration()) as? JetFunctionLiteral
            return function == null || !function.isInsideOf(originalElements)
        }

        if (originalStartOffset != null) {
            val resultMap = HashMap<Int, ResolveResult>()

            for ((ref, context) in JetFileReferencesResolver.resolve(originalFile, getExpressions())) {
                if (ref !is JetSimpleNameExpression) continue

                val resolvedCall = ref.getResolvedCall(context)?.let {
                    (it as? VariableAsFunctionResolvedCall)?.functionCall ?: it
                }

                val descriptor = context[BindingContext.REFERENCE_TARGET, ref]
                if (descriptor == null) continue

                val declaration = DescriptorToDeclarationUtil.getDeclaration(project, descriptor) as? PsiNamedElement
                        ?: if (isExtractableIt(descriptor, context)) itFakeDeclaration else continue

                val offset = ref.getTextRange()!!.getStartOffset() - originalStartOffset
                resultMap[offset] = ResolveResult(ref, declaration, descriptor, resolvedCall)
            }
            resultMap
        }
        else Collections.emptyMap<Int, ResolveResult>()
    }

    fun getBrokenReferencesInfo(body: JetBlockExpression): List<ResolvedReferenceInfo> {
        val startOffset = body.getBlockContentOffset()

        val referencesInfo = ArrayList<ResolvedReferenceInfo>()
        val refToContextMap = JetFileReferencesResolver.resolve(body)
        for ((ref, context) in refToContextMap) {
            if (ref !is JetSimpleNameExpression) continue

            val offset = ref.getTextRange()!!.getStartOffset() - startOffset
            val originalResolveResult = refOffsetToDeclaration[offset]
            if (originalResolveResult == null) continue

            val parent = ref.getParent()
            if (parent is JetQualifiedExpression && parent.getSelectorExpression() == ref) {
                val receiverDescriptor =
                        (originalResolveResult.resolvedCall?.getDispatchReceiver() as? ThisReceiver)?.getDeclarationDescriptor()
                if ((receiverDescriptor as? ClassDescriptor)?.getKind() != ClassKind.CLASS_OBJECT
                        && parent.getReceiverExpression() !is JetSuperExpression) continue
            }
            // Skip P in type references like 'P.Q'
            if (parent is JetUserType && (parent.getParent() as? JetUserType)?.getQualifier() == parent) continue

            val descriptor = context[BindingContext.REFERENCE_TARGET, ref]
            if (!compareDescriptors(originalResolveResult.descriptor, descriptor)
                    && !originalResolveResult.declaration.isInsideOf(originalElements)) {
                referencesInfo.add(ResolvedReferenceInfo(ref, offset, originalResolveResult))
            }
        }

        return referencesInfo
    }
}

// Hack:
// we can't get first element offset through getStatement()/getChildren() since they skip comments and whitespaces
// So we take offset of the left brace instead and increase it by 2 (which is length of "{\n" separating block start and its first element)
private fun JetExpression.getBlockContentOffset(): Int {
    (this as? JetBlockExpression)?.getLBrace()?.let {
        return it.getTextRange()!!.getStartOffset() + 2
    }
    return getTextRange()!!.getStartOffset()
}
