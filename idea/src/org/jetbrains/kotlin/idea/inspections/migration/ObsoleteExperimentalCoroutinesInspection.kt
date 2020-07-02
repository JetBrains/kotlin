/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

internal class ObsoleteExperimentalCoroutinesInspection : ObsoleteCodeMigrationInspection() {
    override val fromVersion: LanguageVersion = LanguageVersion.KOTLIN_1_2
    override val toVersion: LanguageVersion = LanguageVersion.KOTLIN_1_3

    override val problemReporters = listOf(
        ObsoleteTopLevelFunctionUsageReporter(
            "buildSequence",
            "kotlin.coroutines.experimental.buildSequence",
            "kotlin.sequences.sequence"
        ),
        ObsoleteTopLevelFunctionUsageReporter(
            "buildIterator",
            "kotlin.coroutines.experimental.buildIterator",
            "kotlin.sequences.iterator"
        ),
        ObsoleteExtensionFunctionUsageReporter(
            "resume",
            "kotlin.coroutines.experimental.Continuation.resume",
            "kotlin.coroutines.resume"
        ),
        ObsoleteExtensionFunctionUsageReporter(
            "resumeWithException",
            "kotlin.coroutines.experimental.Continuation.resumeWithException",
            "kotlin.coroutines.resumeWithException"
        ),
        ObsoleteCoroutinesImportsUsageReporter
    )
}

private object ObsoleteCoroutinesUsageInWholeProjectFix : ObsoleteCodeInWholeProjectFix() {
    override val inspectionName = ObsoleteExperimentalCoroutinesInspection().shortName
    override fun getFamilyName(): String = KotlinBundle.message("obsolete.coroutine.usage.in.whole.fix.family.name")
}

private class ObsoleteCoroutinesDelegateQuickFix(delegate: ObsoleteCodeFix) : ObsoleteCodeFixDelegateQuickFix(delegate) {
    override fun getFamilyName(): String = KotlinBundle.message("obsolete.coroutine.usage.fix.family.name")
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

internal fun fixesWithWholeProject(isOnTheFly: Boolean, fix: LocalQuickFix, wholeProjectFix: LocalQuickFix): Array<LocalQuickFix> {
    if (!isOnTheFly) {
        return arrayOf(fix)
    }

    return arrayOf(fix, wholeProjectFix)
}

private class ObsoleteTopLevelFunctionUsageReporter(
    val textMarker: String, val oldFqName: String, val newFqName: String
) : ObsoleteCodeProblemReporter {
    override fun report(holder: ProblemsHolder, isOnTheFly: Boolean, simpleNameExpression: KtSimpleNameExpression): Boolean {
        if (simpleNameExpression.text != textMarker) return false

        if (!isTopLevelCallForReplace(simpleNameExpression, oldFqName, newFqName)) {
            return false
        }

        holder.registerProblem(
            simpleNameExpression,
            KotlinBundle.message("0.is.expected.to.be.used.since.kotlin.1.3", newFqName),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            *fixesWithWholeProject(isOnTheFly, ObsoleteCoroutinesDelegateQuickFix(fix), ObsoleteCoroutinesUsageInWholeProjectFix)
        )

        return true
    }

    private val fix = RebindReferenceFix(newFqName)

    companion object {
        private class RebindReferenceFix(val fqName: String) : ObsoleteCodeFix {
            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                val element = descriptor.psiElement
                if (element !is KtSimpleNameExpression) return

                element.mainReference.bindToFqName(FqName(fqName), KtSimpleNameReference.ShorteningMode.DELAYED_SHORTENING)

                performDelayedRefactoringRequests(project)
            }
        }
    }
}

private class ObsoleteExtensionFunctionUsageReporter(
    val textMarker: String, val oldFqName: String, val newFqName: String
) : ObsoleteCodeProblemReporter {
    override fun report(holder: ProblemsHolder, isOnTheFly: Boolean, simpleNameExpression: KtSimpleNameExpression): Boolean {
        if (simpleNameExpression.text != textMarker) return false

        if (!isTopLevelCallForReplace(simpleNameExpression, oldFqName, newFqName)) {
            return false
        }

        holder.registerProblem(
            simpleNameExpression,
            KotlinBundle.message("methods.are.absent.in.coroutines.class.since.1.3"),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            *fixesWithWholeProject(isOnTheFly, ObsoleteCoroutinesDelegateQuickFix(fix), ObsoleteCoroutinesUsageInWholeProjectFix)
        )

        return true
    }

    private val fix = ImportExtensionFunctionFix(newFqName)

    companion object {
        private class ImportExtensionFunctionFix(val fqName: String) : ObsoleteCodeFix {
            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                val element = descriptor.psiElement
                if (element !is KtSimpleNameExpression) return

                val importFun =
                    KotlinTopLevelFunctionFqnNameIndex.getInstance()
                        .get(fqName, element.project, GlobalSearchScope.allScope(element.project))
                        .asSequence()
                        .map { it.resolveToDescriptorIfAny() }
                        .find { it != null && it.importableFqName?.asString() == fqName } ?: return

                ImportInsertHelper.getInstance(element.project).importDescriptor(
                    element.containingKtFile,
                    importFun,
                    forceAllUnderImport = false
                )
            }
        }
    }
}

private object ObsoleteCoroutinesImportsUsageReporter : ObsoleteImportsUsageReporter() {
    override val textMarker: String = "experimental"

    override val packageBindings: Map<String, String> = mapOf(
        "kotlinx.coroutines.experimental" to "kotlinx.coroutines",
        "kotlin.coroutines.experimental" to "kotlin.coroutines"
    )
    override val importsToRemove: Set<String> = setOf(
        "kotlin.coroutines.experimental.buildSequence",
        "kotlin.coroutines.experimental.buildIterator"
    )

    override val wholeProjectFix: LocalQuickFix = ObsoleteCoroutinesUsageInWholeProjectFix
    override fun problemMessage(): String = KotlinBundle.message("experimental.coroutines.usages.are.obsolete.since.1.3")
    override fun wrapFix(fix: ObsoleteCodeFix): LocalQuickFix = ObsoleteCoroutinesDelegateQuickFix(fix)
}

