/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors.SUPERTYPE_NOT_INITIALIZED
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

class KotlinAndroidViewConstructorFix(element: KtSuperTypeEntry) : KotlinQuickFixAction<KtSuperTypeEntry>(element) {

    override fun getText() = "Add Android View constructors using '@JvmOverloads'"
    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        return AndroidFacet.getInstance(file) != null
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val ktClass = element.containingClass() ?: return

        val factory = KtPsiFactory(element)

        val newPrimaryConstructor = factory.createPrimaryConstructor(
            """(
            context: android.content.Context, attrs: android.util.AttributeSet? = null, defStyleAttr: Int = 0
            )""".trimIndent()
        )

        val primaryConstructor = ktClass.createPrimaryConstructorIfAbsent().replaced(newPrimaryConstructor)
        primaryConstructor.valueParameterList?.let { ShortenReferences.DEFAULT.process(it) }
        primaryConstructor.addAnnotation(fqNameAnnotation)

        element.replace(factory.createSuperTypeCallEntry(element.text + "(context, attrs, defStyleAttr)"))
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {

        private val fqNameAnnotation = FqName("kotlin.jvm.JvmOverloads")

        private val requiredConstructorParameterTypes =
            listOf("android.content.Context", "android.util.AttributeSet", "kotlin.Int")

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val superTypeEntry = SUPERTYPE_NOT_INITIALIZED.cast(diagnostic).psiElement

            val ktClass = superTypeEntry.containingClass() ?: return null
            if (ktClass.primaryConstructor != null) return null

            val context = superTypeEntry.analyze()
            val type = superTypeEntry.typeReference?.let { context[BindingContext.TYPE, it] } ?: return null

            if (!type.isAndroidView() && type.supertypes().none { it.isAndroidView() }) return null

            val names = type.constructorParameters() ?: return null
            if (requiredConstructorParameterTypes !in names) return null

            return KotlinAndroidViewConstructorFix(superTypeEntry)
        }

        private fun KotlinType.getFqNameAsString() = constructor.declarationDescriptor?.fqNameUnsafe?.asString()

        private fun KotlinType.isAndroidView() = getFqNameAsString() == "android.view.View"

        private fun KotlinType.constructorParameters(): List<List<String?>>? {
            val classDescriptor = constructor.declarationDescriptor as? ClassDescriptor ?: return null
            return classDescriptor.constructors.map {
                it.valueParameters.map { parameter -> parameter.type.getFqNameAsString() }
            }
        }
    }
}


