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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.openapi.project.Project
import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.openapi.util.TextRange
import kotlin.properties.Delegates
import java.util.HashMap
import org.jetbrains.jet.plugin.codeInsight.JetFileReferencesResolver
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil
import java.util.Collections
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.psi.psiUtil.isInsideOf
import java.util.ArrayList
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.lang.psi.JetSuperExpression
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody
import org.jetbrains.jet.lang.psi.JetUserType
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetFunctionLiteral
import org.jetbrains.jet.lang.psi.JetClassInitializer
import org.jetbrains.jet.lang.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils

data class ExtractionOptions(val inferUnitTypeForUnusedValues: Boolean) {
    class object {
        val DEFAULT = ExtractionOptions(true)
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

class ExtractionData(
        val originalFile: JetFile,
        val originalElements: List<PsiElement>,
        val targetSibling: PsiElement,
        val options: ExtractionOptions = ExtractionOptions.DEFAULT
) {
    val project: Project = originalFile.getProject()

    val insertBefore: Boolean = targetSibling.getParentByType(javaClass<JetDeclaration>(), true)?.let {
        it is JetDeclarationWithBody || it is JetClassInitializer
    } ?: false

    fun getExpressions(): List<JetExpression> = originalElements.filterIsInstance(javaClass<JetExpression>())

    fun getCodeFragmentTextRange(): TextRange? {
        val originalElements = originalElements
        return when (originalElements.size) {
            0 -> null
            1 -> originalElements.first!!.getTextRange()
            else -> {
                val from = originalElements.first!!.getTextRange()!!.getStartOffset()
                val to = originalElements.last!!.getTextRange()!!.getEndOffset()
                TextRange(from, to)
            }
        }
    }

    fun getCodeFragmentText(): String =
            getCodeFragmentTextRange()?.let { originalFile.getText()?.substring(it.getStartOffset(), it.getEndOffset()) } ?: ""

    val originalStartOffset = originalElements.first?.let { e -> e.getTextRange()!!.getStartOffset() }

    private val itFakeDeclaration by Delegates.lazy { JetPsiFactory(originalFile).createParameter("it", "Any?") }

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
                        (originalResolveResult.resolvedCall?.getThisObject() as? ThisReceiver)?.getDeclarationDescriptor()
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

private fun compareDescriptors(d1: DeclarationDescriptor?, d2: DeclarationDescriptor?): Boolean {
    return d1 == d2 ||
            (d1 != null && d2 != null &&
                    DescriptorRenderer.FQ_NAMES_IN_TYPES.render(d1) == DescriptorRenderer.FQ_NAMES_IN_TYPES.render(d2))
}

// Hack:
// we can't get first element offset through getStatement()/getChildren() since they skip comments and whitespaces
// So we take offset of the left brace instead and increase it by 2 (which is length of "{\n" separating block start and its first element)
private fun JetBlockExpression.getBlockContentOffset(): Int {
    return getLBrace()!!.getTextRange()!!.getStartOffset() + 2
}