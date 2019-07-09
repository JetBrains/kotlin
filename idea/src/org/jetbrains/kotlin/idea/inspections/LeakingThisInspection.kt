/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import org.jetbrains.kotlin.cfg.LeakingThisDescriptor.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.quickfix.AddModifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext.LEAKING_THIS
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class LeakingThisInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return classVisitor { klass ->
            // We still use analyzeWithAllCompilerChecks() here.
            // It's possible to use analyze(), but then we should repeat class constructor consistency check
            // for different class internal elements, like KtProperty and KtClassInitializer.
            // It can affect performance, so yet we want to avoid this.
            val context = klass.analyzeWithContent()
            val leakingThese = context.getSliceContents(LEAKING_THIS)
            these@ for ((expression, leakingThisDescriptor) in leakingThese) {
                if (leakingThisDescriptor.classOrObject != klass) continue@these
                val description: String = when (leakingThisDescriptor) {
                    is NonFinalClass ->
                        if (expression is KtThisExpression && expression.getStrictParentOfType<KtClassLiteralExpression>() == null) {
                            val name = leakingThisDescriptor.klass.name
                            klass.createDescription(
                                "Leaking 'this' in constructor of non-final class $name",
                                "Leaking 'this' in constructor of enum class $name (with overridable members)"
                            ) { it.hasOverriddenMember() } ?: continue@these
                        } else {
                            continue@these // Not supported yet
                        }
                    is NonFinalProperty -> {
                        val name = leakingThisDescriptor.property.name.asString()
                        klass.createDescription("Accessing non-final property $name in constructor") {
                            it.hasOverriddenMember { owner -> owner.name == name }
                        } ?: continue@these
                    }
                    is NonFinalFunction -> {
                        val function = leakingThisDescriptor.function
                        klass.createDescription("Calling non-final function ${function.name} in constructor") {
                            it.hasOverriddenMember { owner ->
                                owner is KtNamedFunction && owner.name == function.name.asString() && owner.valueParameters.size == function.valueParameters.size
                            }
                        } ?: continue@these
                    }
                    else -> continue@these // Not supported yet
                }
                val memberDescriptorToFix = when (leakingThisDescriptor) {
                    is NonFinalProperty -> leakingThisDescriptor.property
                    is NonFinalFunction -> leakingThisDescriptor.function
                    else -> null
                }
                val memberFix = memberDescriptorToFix?.let {
                    if (it.modality == Modality.OPEN) {
                        val modifierListOwner = DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtDeclaration
                        createMakeFinalFix(modifierListOwner)
                    } else null
                }

                val classFix =
                    if (klass.hasModifier(KtTokens.OPEN_KEYWORD)) {
                        createMakeFinalFix(klass)
                    } else null

                holder.registerProblem(
                    expression, description,
                    when (leakingThisDescriptor) {
                        is NonFinalProperty, is NonFinalFunction -> GENERIC_ERROR_OR_WARNING
                        else -> WEAK_WARNING
                    },
                    *(arrayOf(memberFix, classFix).filterNotNull().toTypedArray())
                )
            }
        }
    }

    companion object {
        private fun createMakeFinalFix(declaration: KtDeclaration?): IntentionWrapper? {
            declaration ?: return null
            val useScope = declaration.useScope
            if (DefinitionsScopedSearch.search(declaration, useScope).findFirst() != null) return null
            if ((declaration.containingClassOrObject as? KtClass)?.isInterface() == true) return null
            return IntentionWrapper(AddModifierFix(declaration, KtTokens.FINAL_KEYWORD), declaration.containingFile)
        }
    }
}

private fun KtClass.createDescription(
    defaultText: String,
    enumText: String = defaultText,
    check: (KtClass) -> Boolean
): String? {
    return if (isEnum()) {
        if (check(this)) return null
        enumText
    } else {
        defaultText
    }
}

private fun KtEnumEntry.hasOverriddenMember(additionalCheck: (KtDeclaration) -> Boolean): Boolean = declarations.any {
    it.hasModifier(KtTokens.OVERRIDE_KEYWORD) && additionalCheck(it)
}

private fun KtClass.hasOverriddenMember(filter: (KtDeclaration) -> Boolean = { true }): Boolean {
    val enumEntries = body?.getChildrenOfType<KtEnumEntry>() ?: return false
    return enumEntries.none {
        it.hasOverriddenMember(filter)
    }
}