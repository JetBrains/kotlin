/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.*
import com.intellij.codeInspection.actions.RunInspectionIntention
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.codeInspection.ex.InspectionToolWrapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.configuration.isLanguageVersionUpdate
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.migration.MigrationFix
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
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

            for (registeredProblem in PROBLEMS) {
                if (registeredProblem.report(holder, isOnTheFly, simpleNameExpression)) {
                    return
                }
            }
        })
    }

    companion object {
        private val PROBLEMS = listOf(
            ObsoleteTopLevelFunctionUsage(
                "buildSequence",
                "kotlin.coroutines.experimental.buildSequence",
                "kotlin.sequences.sequence"
            ),
            ObsoleteTopLevelFunctionUsage(
                "buildIterator",
                "kotlin.coroutines.experimental.buildIterator",
                "kotlin.sequences.iterator"
            ),
            ObsoleteExtensionFunctionUsage(
                "resume",
                "kotlin.coroutines.experimental.Continuation.resume",
                "kotlin.coroutines.resume"
            ),
            ObsoleteExtensionFunctionUsage(
                "resumeWithException",
                "kotlin.coroutines.experimental.Continuation.resumeWithException",
                "kotlin.coroutines.resumeWithException"
            ),
            ExperimentalImportUsage()
        )
    }
}

private interface CoroutineMigrationProblem {
    fun report(holder: ProblemsHolder, isOnTheFly: Boolean, simpleNameExpression: KtSimpleNameExpression): Boolean
}

// Shortcut quick fix for running inspection in the project scope.
// Should work like RunInspectionAction.runInspection.
private class ObsoleteCoroutineUsageInWholeFix : LocalQuickFix {
    override fun getFamilyName(): String = "Fix experimental coroutines usages in the project"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val toolWrapper = InspectionProjectProfileManager.getInstance(project).currentProfile.getInspectionTool(
            ObsoleteExperimentalCoroutinesInspection().shortName,
            project
        )!!

        runToolInProject(project, toolWrapper)
    }

    override fun startInWriteAction(): Boolean = false

    companion object {
        val INSTANCE = ObsoleteCoroutineUsageInWholeFix()

        private fun runToolInProject(project: Project, toolWrapper: InspectionToolWrapper<*, *>) {
            val managerEx = InspectionManager.getInstance(project) as InspectionManagerEx
            val kotlinSourcesScope: GlobalSearchScope = KotlinSourceFilterScope.projectSources(GlobalSearchScope.allScope(project), project)
            val cleanupScope = AnalysisScope(kotlinSourcesScope, project)

            val cleanupToolProfile = runInInspectionProfileInitMode { RunInspectionIntention.createProfile(toolWrapper, managerEx, null) }
            managerEx.createNewGlobalContext(false)
                .codeCleanup(cleanupScope, cleanupToolProfile, "Apply in the project: " + toolWrapper.displayName, null, false)
        }

        // Overcome failure during profile creating because of absent tools in tests
        inline fun <T> runInInspectionProfileInitMode(runnable: () -> T): T {
            return if (!ApplicationManager.getApplication().isUnitTestMode) {
                runnable()
            } else {
                val old = InspectionProfileImpl.INIT_INSPECTIONS
                try {
                    InspectionProfileImpl.INIT_INSPECTIONS = true
                    runnable()
                } finally {
                    InspectionProfileImpl.INIT_INSPECTIONS = old
                }
            }
        }
    }
}

/**
 * There should be a single fix class with the same family name, this way it can be executed for all found coroutines problems from UI.
 */
private class ObsoleteCoroutineUsageFix(val delegate: CoroutineFix) : LocalQuickFix {
    override fun getFamilyName(): String = "Fix experimental coroutines usage"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        delegate.applyFix(project, descriptor)
    }

    companion object {
        interface CoroutineFix {
            fun applyFix(project: Project, descriptor: ProblemDescriptor)
        }
    }
}

private fun isTopLevelCallForReplace(simpleNameExpression: KtSimpleNameExpression, oldFqName: String, newFqName: String): Boolean {
    if (simpleNameExpression.parent !is KtCallExpression) return false

    val descriptor = simpleNameExpression.resolveMainReferenceToDescriptors().firstOrNull() ?: return false
    val callableDescriptor = descriptor as? CallableDescriptor ?: return false

    val resolvedToFqName = callableDescriptor.fqNameOrNull()?.asString() ?: return false
    if (resolvedToFqName != oldFqName) return false

    val project = simpleNameExpression.project

    val isInIndex = KotlinTopLevelFunctionFqnNameIndex.getInstance()
        .get(newFqName, project, GlobalSearchScope.allScope(project))
        .isEmpty()

    return !isInIndex
}

private fun fixesWithWholeProject(isOnTheFly: Boolean, fix: LocalQuickFix): Array<LocalQuickFix> {
    if (!isOnTheFly) {
        return arrayOf(fix)
    }

    return arrayOf(fix, ObsoleteCoroutineUsageInWholeFix.INSTANCE)
}

private class ObsoleteTopLevelFunctionUsage(
    val textMarker: String, val oldFqName: String, val newFqName: String
) : CoroutineMigrationProblem {
    override fun report(holder: ProblemsHolder, isOnTheFly: Boolean, simpleNameExpression: KtSimpleNameExpression): Boolean {
        if (simpleNameExpression.text != textMarker) return false

        if (!isTopLevelCallForReplace(simpleNameExpression, oldFqName, newFqName)) {
            return false
        }

        holder.registerProblem(
            simpleNameExpression,
            "`$newFqName` is expected to be used since Kotlin 1.3",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            *fixesWithWholeProject(isOnTheFly, ObsoleteCoroutineUsageFix(fix))
        )

        return true
    }

    private val fix = RebindReferenceFix(newFqName)

    companion object {
        private class RebindReferenceFix(val fqName: String) : ObsoleteCoroutineUsageFix.Companion.CoroutineFix {
            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                val element = descriptor.psiElement
                if (element !is KtSimpleNameExpression) return

                element.mainReference.bindToFqName(FqName(fqName), KtSimpleNameReference.ShorteningMode.DELAYED_SHORTENING)

                performDelayedRefactoringRequests(project)
            }
        }
    }
}

private class ObsoleteExtensionFunctionUsage(
    val textMarker: String, val oldFqName: String, val newFqName: String
) : CoroutineMigrationProblem {
    override fun report(holder: ProblemsHolder, isOnTheFly: Boolean, simpleNameExpression: KtSimpleNameExpression): Boolean {
        if (simpleNameExpression.text != textMarker) return false

        if (!isTopLevelCallForReplace(simpleNameExpression, oldFqName, newFqName)) {
            return false
        }

        holder.registerProblem(
            simpleNameExpression,
            "Methods are absent in coroutines class since 1.3",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            *fixesWithWholeProject(isOnTheFly, ObsoleteCoroutineUsageFix(fix))
        )

        return true
    }

    private val fix = ImportExtensionFunctionFix(newFqName)

    companion object {
        private class ImportExtensionFunctionFix(val fqName: String) : ObsoleteCoroutineUsageFix.Companion.CoroutineFix {
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

private class ExperimentalImportUsage : CoroutineMigrationProblem {
    override fun report(holder: ProblemsHolder, isOnTheFly: Boolean, simpleNameExpression: KtSimpleNameExpression): Boolean {
        if (simpleNameExpression.text != EXPERIMENTAL_COROUTINES_MARKER) return false

        val parent = simpleNameExpression.parent as? KtExpression ?: return false
        val reportExpression = parent as? KtDotQualifiedExpression ?: simpleNameExpression

        findBinding(simpleNameExpression) ?: return false

        holder.registerProblem(
            reportExpression,
            "Experimental coroutines usages are obsolete since 1.3",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            *fixesWithWholeProject(isOnTheFly, ObsoleteCoroutineUsageFix(ObsoleteCoroutineImportFix))
        )

        return true
    }

    companion object {
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
            "kotlin.coroutines.experimental.buildSequence",
            "kotlin.coroutines.experimental.buildIterator"
        )

        private const val EXPERIMENTAL_COROUTINES_MARKER = "experimental"

        private fun findBinding(simpleNameExpression: KtSimpleNameExpression): Binding? {
            if (simpleNameExpression.text != EXPERIMENTAL_COROUTINES_MARKER) return null

            val importDirective = simpleNameExpression.parents
                .takeWhile { it is KtDotQualifiedExpression || it is KtImportDirective }
                .lastOrNull() as? KtImportDirective ?: return null

            val fqNameStr = importDirective.importedFqName?.asString() ?: return null

            val bindEntry = PACKAGE_BINDING.entries.find { (affectedImportPrefix, _) ->
                fqNameStr.startsWith(affectedImportPrefix)
            } ?: return null

            return Binding(
                FqName(bindEntry.value),
                fqNameStr in IMPORTS_TO_REMOVE,
                importDirective
            )
        }

        private object ObsoleteCoroutineImportFix : ObsoleteCoroutineUsageFix.Companion.CoroutineFix {
            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                val element = descriptor.psiElement
                val simpleNameExpression = when (element) {
                    is KtSimpleNameExpression -> element
                    is KtDotQualifiedExpression -> element.selectorExpression as? KtSimpleNameExpression
                    else -> null
                } ?: return

                val binding = findBinding(simpleNameExpression) ?: return

                if (binding.shouldRemove) {
                    binding.importDirective.delete()
                } else {
                    simpleNameExpression.mainReference.bindToFqName(
                        binding.bindTo, shorteningMode = KtSimpleNameReference.ShorteningMode.NO_SHORTENING
                    )
                }
            }
        }
    }
}
