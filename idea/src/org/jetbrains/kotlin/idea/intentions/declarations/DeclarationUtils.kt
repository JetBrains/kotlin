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

package org.jetbrains.kotlin.idea.intentions.declarations

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.JetType

public object DeclarationUtils {

    private fun assertNotNull(value: Any?) {
        assert(value != null, "Expression must be checked before applying transformation")
    }

    public fun checkSplitProperty(property: JetProperty): Boolean {
        return property.hasInitializer() && property.isLocal()
    }

    private fun getPropertyTypeIfNeeded(property: JetProperty): JetType? {
        if (property.getTypeReference() != null) return null

        val initializer = property.getInitializer()
        val type = if (initializer != null) property.analyze(BodyResolveMode.FULL).getType(initializer) else null
        return if (type == null || type.isError()) null else type
    }

    // returns assignment which replaces initializer
    public fun splitPropertyDeclaration(property: JetProperty): JetBinaryExpression {
        var property = property
        val parent = property.getParent()
        assertNotNull(parent)

        //noinspection unchecked
        val initializer = property.getInitializer()
        assertNotNull(initializer)

        val psiFactory = JetPsiFactory(property)
        //noinspection ConstantConditions, unchecked
        var newInitializer = psiFactory.createExpressionByPattern("$0 = $1", property.getName(), initializer)

        newInitializer = parent.addAfter(newInitializer, property) as JetBinaryExpression
        parent.addAfter(psiFactory.createNewLine(), property)

        val project = newInitializer.getProject()
        val file = parent.getContainingFile()
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(PsiDocumentManager.getInstance(project).getDocument(file))

        //noinspection ConstantConditions
        val inferredType = getPropertyTypeIfNeeded(property)

        val typeStr = if (inferredType != null)
            IdeDescriptorRenderers.SOURCE_CODE.renderType(inferredType)
        else
            JetPsiUtil.getNullableText(property.getTypeReference())

        //noinspection ConstantConditions
        property = property.replace(psiFactory.createProperty(property.getNameIdentifier()!!.getText(), typeStr, property.isVar())) as JetProperty

        if (inferredType != null) {
            ShortenReferences.DEFAULT.process(property.getTypeReference())
        }

        return newInitializer
    }
}
