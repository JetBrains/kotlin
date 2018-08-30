/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.codeInspection.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.configuration.isLanguageVersionUpdate
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.migration.MigrationFix
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

class ObsoleteExperimentalCoroutinesInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool, MigrationFix {
    override fun isApplicable(migrationInfo: MigrationInfo): Boolean {
        return migrationInfo.isLanguageVersionUpdate(LanguageVersion.KOTLIN_1_2, LanguageVersion.KOTLIN_1_3)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): KtVisitorVoid {
        return simpleNameExpressionVisitor(fun(simpleNameExpression) {
            run {
                val versionAtLeast13 = simpleNameExpression.languageVersionSettings.languageVersion >= LanguageVersion.KOTLIN_1_3
                if (!versionAtLeast13 && !ApplicationManager.getApplication().isUnitTestMode) {
                    return
                }
            }

            when (simpleNameExpression.text) {
                RESUME_MARKER, RESUME_WITH_EXCEPTION_MARKER -> {
                    if (simpleNameExpression.parent !is KtCallExpression) return

                    val descriptor = simpleNameExpression.resolveMainReferenceToDescriptors().firstOrNull() ?: return
                    val callableDescriptor = descriptor as? CallableDescriptor ?: return

                    val resolvedToFqName = callableDescriptor.fqNameOrNull()?.asString() ?: return
                    val fixFqName = when (resolvedToFqName) {
                        RESUME_FUNCTION_FQNAME -> RESUME_FUNCTION_FIX_FQNAME
                        RESUME_WITH_EXCEPTION_FQNAME -> RESUME_WITH_EXCEPTION_FIX_FQNAME
                        else -> null
                    } ?: return

                    val problemDescriptor = holder.manager.createProblemDescriptor(
                        simpleNameExpression,
                        simpleNameExpression,
                        "Methods are absent in coroutines class since 1.3",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        isOnTheFly,
                        ObsoleteCoroutineUsageFix.createExtensionFix(fixFqName)
                    )

                    holder.registerProblem(problemDescriptor)
                }

                EXPERIMENTAL_COROUTINES_MARKER -> {
                    val parent = simpleNameExpression.parent as? KtExpression ?: return
                    val reportExpression = parent as? KtDotQualifiedExpression ?: simpleNameExpression

                    findBinding(simpleNameExpression) ?: return

                    val problemDescriptor = holder.manager.createProblemDescriptor(
                        reportExpression,
                        reportExpression,
                        "Experimental coroutines usages are obsolete since 1.3",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        isOnTheFly,
                        ObsoleteCoroutineUsageFix.createImportFix()
                    )

                    holder.registerProblem(problemDescriptor)
                }
            }
        })
    }

    companion object {
        private const val EXPERIMENTAL_COROUTINES_MARKER = "experimental"

        private const val RESUME_MARKER = "resume"
        private const val RESUME_FUNCTION_FQNAME = "kotlin.coroutines.experimental.Continuation.resume"
        private const val RESUME_FUNCTION_FIX_FQNAME = "kotlin.coroutines.resume"

        private const val RESUME_WITH_EXCEPTION_MARKER = "resumeWithException"
        private const val RESUME_WITH_EXCEPTION_FQNAME = "kotlin.coroutines.experimental.Continuation.resumeWithException"
        private const val RESUME_WITH_EXCEPTION_FIX_FQNAME = "kotlin.coroutines.resumeWithException"

        private class Binding(
            val bindTo: FqName,
            val shouldRemove: Boolean,
            val importDirective: KtImportDirective
        )

        @Suppress("SpellCheckingInspection")
        private val PACKAGE_BINDING = mapOf(
            "kotlinx.coroutines.experimental" to "kotlinx.coroutines",
            "kotlin.coroutines.experimental" to "kotlin.coroutines"
        )

        private val IMPORTS_TO_REMOVE = setOf(
            "kotlin.coroutines.experimental.buildSequence"
        )

        private fun findBinding(simpleNameExpression: KtSimpleNameExpression): Binding? {
            if (simpleNameExpression.text != EXPERIMENTAL_COROUTINES_MARKER) return null

            val importDirective = simpleNameExpression.parents
                .takeWhile { it is KtDotQualifiedExpression || it is KtImportDirective }
                .lastOrNull() as? KtImportDirective ?: return null

            val fqNameStr = importDirective.importedFqName?.asString() ?: return null

            val bindEntry = PACKAGE_BINDING.entries.find { (affectedImportPrefix, _) ->
                fqNameStr.startsWith(affectedImportPrefix)
            } ?: return null

            return Binding(FqName(bindEntry.value), fqNameStr in IMPORTS_TO_REMOVE, importDirective)
        }
    }

    private class ObsoleteCoroutineUsageFix(val delegate: LocalQuickFix) : LocalQuickFix {
        override fun getFamilyName(): String = QUICK_FIX_NAME

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            delegate.applyFix(project, descriptor)
        }

        companion object {
            private const val QUICK_FIX_NAME = "Fix experimental coroutines usage"

            fun createImportFix() = ObsoleteCoroutineUsageFix(ObsoleteCoroutineImportFix())
            fun createExtensionFix(fqName: String) = ObsoleteCoroutineUsageFix(ImportExtensionFunctionFix(fqName))

            private class ObsoleteCoroutineImportFix : LocalQuickFix {
                override fun getFamilyName() = QUICK_FIX_NAME

                override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                    val element = descriptor.psiElement
                    val simpleNameExpression = when (element) {
                        is KtSimpleNameExpression -> element
                        is KtDotQualifiedExpression -> element.selectorExpression as? KtSimpleNameExpression
                        else -> null
                    } ?: return

                    val binding = ObsoleteExperimentalCoroutinesInspection.findBinding(simpleNameExpression) ?: return

                    if (binding.shouldRemove) {
                        binding.importDirective.delete()
                    } else {
                        simpleNameExpression.mainReference.bindToFqName(
                            binding.bindTo, shorteningMode = KtSimpleNameReference.ShorteningMode.NO_SHORTENING
                        )
                    }
                }
            }

            private class ImportExtensionFunctionFix(val fqName: String) : LocalQuickFix {
                override fun getFamilyName() = QUICK_FIX_NAME

                override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                    val element = descriptor.psiElement
                    if (element !is KtSimpleNameExpression) return

                    val importFun =
                        KotlinTopLevelFunctionFqnNameIndex.getInstance()
                            .get(fqName, element.project, GlobalSearchScope.allScope(element.project))
                            .asSequence()
                            .map { it.resolveToDescriptorIfAny() }
                            .find { it != null && it.importableFqName?.asString() == fqName } ?: return

                    ImportInsertHelper.getInstance(element.project).importDescriptor(element.containingKtFile, importFun, false)
                }
            }
        }
    }
}
