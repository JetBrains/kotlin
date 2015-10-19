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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.KotlinLightClass
import org.jetbrains.kotlin.asJava.KotlinLightFieldForDeclaration
import org.jetbrains.kotlin.idea.quickfix.AddConstModifierFix
import org.jetbrains.kotlin.idea.quickfix.AddConstModifierIntention
import org.jetbrains.kotlin.idea.quickfix.replaceReferencesToGetterByReferenceToField
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.util.*

class DeprecatedUsageOfStaticFieldInspection : LocalInspectionTool(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                val resolvedTo = expression.reference?.resolve() as? PsiField ?: return
                if (!resolvedTo.hasModifierProperty(PsiModifier.STATIC) || !resolvedTo.isDeprecated) return

                val kotlinProperty = (resolvedTo as? KotlinLightFieldForDeclaration)?.getOrigin() as? KtProperty

                // NOTE: this is hack to avoid test failing with "action is still available" error
                if (kotlinProperty?.hasJvmFieldAnnotationOrConstModifier() ?: false) return

                val kotlinClassOrObject = (resolvedTo.containingClass as? KotlinLightClass)?.getOrigin() ?: return

                val containingObject = when (kotlinClassOrObject) {
                    is KtObjectDeclaration -> kotlinClassOrObject as KtObjectDeclaration // KT-9578
                    is KtClass -> kotlinClassOrObject.getCompanionObjects().singleOrNull() ?: return
                    else -> return
                }
                holder.registerProblem(
                        expression, "This field will not be generated in future versions of Kotlin. Use 'const' modifier, '@JvmField' annotation or access data through corresponding object.",
                        ProblemHighlightType.LIKE_DEPRECATED,
                        *createFixes(containingObject, kotlinProperty).toTypedArray()
                )
            }
        }
    }


    private fun createFixes(containingObject: KtObjectDeclaration, property: KtProperty?): List<LocalQuickFix> {
        if (containingObject.getContainingJetFile().isCompiled) return listOf(ReplaceWithGetterInvocationFix())

        // order matters here, 'cleanup' applies fixes in this order
        val fixes = ArrayList<LocalQuickFix>()
        if (property != null && AddConstModifierIntention.isApplicableTo(property)) {
            fixes.add(AddConstModifierLocalFix())
        }

        if (containingObject.isCompanion()) {
            val classWithCompanion = containingObject.parent?.parent as? KtClass ?: return listOf()
            if (!classWithCompanion.isInterface()) {
                fixes.add(AddJvmFieldAnnotationFix())
            }
        } else {
            fixes.add(AddJvmFieldAnnotationFix())
        }

        fixes.add(ReplaceWithGetterInvocationFix())

        return fixes
    }
}

abstract class StaticFieldUsageFix: LocalQuickFix {
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val deprecatedField = descriptor.psiElement.reference?.resolve() as? PsiField ?: return
        val kotlinProperty = (deprecatedField as? KotlinLightFieldForDeclaration)?.getOrigin() as? KtProperty

        if (kotlinProperty != null && kotlinProperty.hasJvmFieldAnnotationOrConstModifier()) return

        doFix(deprecatedField, kotlinProperty, descriptor)
    }

    abstract fun doFix(deprecatedField: PsiField, property: KtProperty?, problemDescriptor: ProblemDescriptor)
}

class AddJvmFieldAnnotationFix : StaticFieldUsageFix() {
    override fun doFix(deprecatedField: PsiField, property: KtProperty?, problemDescriptor: ProblemDescriptor) {
        replaceReferencesToGetterByReferenceToField(property ?: return)

        property.addAnnotationEntry(KtPsiFactory(property).createAnnotationEntry("@JvmField"))
    }

    override fun getName(): String = "Annotate property with @JvmField"
    override fun getFamilyName(): String = name
}

class AddConstModifierLocalFix : StaticFieldUsageFix() {
    override fun getName(): String = "Add 'const' modifier to a property"
    override fun getFamilyName(): String = name

    override fun doFix(deprecatedField: PsiField, property: KtProperty?, problemDescriptor: ProblemDescriptor) {
        AddConstModifierFix.addConstModifier(property ?: return)
    }
}

class ReplaceWithGetterInvocationFix : StaticFieldUsageFix() {
    override fun getName(): String = "Replace with getter invocation"
    override fun getFamilyName(): String = name

    override fun doFix(deprecatedField: PsiField, property: KtProperty?, problemDescriptor: ProblemDescriptor) {
        val lightClass = deprecatedField.containingClass as? KotlinLightClass ?: return

        fun replaceWithGetterInvocation(objectField: PsiField) {
            val factory = PsiElementFactory.SERVICE.getInstance(deprecatedField.project)
            val elementToReplace = problemDescriptor.psiElement

            val getterInvocation = factory.createExpressionFromText(
                    objectField.containingClass!!.qualifiedName + "." + objectField.name + "." + JvmAbi.getterName(deprecatedField.name!!) + "()",
                    elementToReplace
            )
            elementToReplace.replace(getterInvocation)
        }

        val kotlinClass = lightClass.getOrigin()
        when (kotlinClass) {
            is KtObjectDeclaration -> {
                val instanceField = lightClass.findFieldByName(JvmAbi.INSTANCE_FIELD, false) ?: return
                replaceWithGetterInvocation(instanceField)
            }
            is KtClass -> {
                val companionObjectName = kotlinClass.getCompanionObjects().singleOrNull()?.name ?: return
                val companionObjectField = lightClass.findFieldByName(companionObjectName, false) ?: return
                replaceWithGetterInvocation(companionObjectField)
            }
        }
    }
}

private fun KtProperty.hasJvmFieldAnnotationOrConstModifier(): Boolean {
    return hasModifier(KtTokens.CONST_KEYWORD) || annotationEntries.any { it.text == "@JvmField" }
}