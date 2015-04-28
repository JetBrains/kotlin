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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.JetType

public class ReconstructTypeInCastOrIsAction : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String {
        return JetBundle.message("replace.by.reconstructed.type.family.name")
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val typeRef = PsiTreeUtil.getTopmostParentOfType<JetTypeReference>(element, javaClass<JetTypeReference>())
        assert(typeRef != null) { "Must be checked by isAvailable(): " + element }

        val type = getReconstructedType(typeRef)
        val newType = JetPsiFactory(typeRef).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
        val replaced = typeRef!!.replace(newType) as JetTypeReference
        ShortenReferences.DEFAULT.process(replaced)
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val typeRef = PsiTreeUtil.getTopmostParentOfType<JetTypeReference>(element, javaClass<JetTypeReference>())
        if (typeRef == null) return false

        // Only user types (like Foo) are interesting
        val typeElement = typeRef.getTypeElement()
        if (typeElement !is JetUserType) return false

        // If there are generic arguments already, there's nothing to reconstruct
        if (!typeElement.getTypeArguments().isEmpty()) return false

        // We must be on the RHS of as/as?/is/!is or inside an is/!is-condition in when()
        val outerExpression = PsiTreeUtil.getParentOfType<JetExpression>(typeRef, javaClass<JetExpression>())
        if (outerExpression !is JetBinaryExpressionWithTypeRHS) {
            val outerIsCondition = PsiTreeUtil.getParentOfType<JetWhenConditionIsPattern>(typeRef, javaClass<JetWhenConditionIsPattern>())
            if (outerIsCondition == null) return false
        }

        val type = getReconstructedType(typeRef)
        if (type == null || type.isError()) return false

        // No type parameters expected => nothing to reconstruct
        if (type.getConstructor().getParameters().isEmpty()) return false

        setText(JetBundle.message("replace.by.reconstructed.type", type))

        return true
    }

    private fun getReconstructedType(typeRef: JetTypeReference): JetType? {
        return typeRef.analyze(BodyResolveMode.FULL).get<JetTypeReference, JetType>(BindingContext.TYPE, typeRef)
    }
}
