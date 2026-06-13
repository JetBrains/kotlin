/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.AnalysisApiNonPublicMarkers.INTERNAL_API_MARKER_ANNOTATIONS
import org.jetbrains.kotlin.AnalysisApiNonPublicMarkers.REQUIRES_OPT_IN
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.test.TestDataAssertions
import java.io.File
import kotlin.test.fail

/**
 * When set to `true`, [AbstractAnalysisApiInternalApiTest] writes the suggested fix to the violating source file instead of asserting via
 * [TestDataAssertions.assertEqualsToFile]. The test then throws to signal the iteration loop in the companion skill
 * `analysis-api-mark-internal-apis`. Combined with `-Pkotlin.test.instrumentation.disable.inputs.check=true`, this lets the skill drive
 * the test through the JVM's security manager.
 */
private val UPDATE_SOURCE_CODE: Boolean
    get() = System.getProperty("kotlin.analysis.codebaseTest.internalApi.updateSourceCode").toBoolean()

/**
 * A base test for verifying that public declarations in an Analysis API implementation module are marked to keep them out of the public
 * Analysis API surface.
 *
 * While an Analysis API client will not necessarily depend on an implementation module, it is good practice for avoiding implementation
 * leaks if such a dependency exists.
 *
 * A declaration is considered marked when any of the following holds:
 *
 *  - It has visibility `internal` or `private`.
 *  - It is annotated with one of `@KaImplementationDetail`, `@KaExperimentalApi`, `@KaPlatformInterface`, `@KaNonPublicApi`, `@KaIdeApi`,
 *    or `@LLFirInternals`.
 *  - It is an `annotation class` carrying `@RequiresOptIn` (these must remain public so callers can `@OptIn(...)`).
 *  - An enclosing classifier has `internal` or `private` visibility (the language already prevents external reach).
 *  - When an enclosing classifier carries one or more marker annotations, every nested classifier must carry the **same** markers
 *    (additionally; nested classifiers may carry more). The Kotlin compiler propagates opt-in requirements lexically, but the binary
 *    compatibility checker does not — so each nesting level must repeat the marker for the API dump to remain honest.
 *
 * Scope:
 *
 *  - Top-level declarations: classes, interfaces, objects, functions, properties, type aliases.
 *  - Nested classifiers (including companion objects, enum classes, sealed classes, annotation classes).
 *  - Out of scope: nested non-classifier members (functions, properties) — their effective visibility is bounded by the parent
 *    classifier; the build also catches "internal exposed through public API" at compile time.
 *
 * The test does not analyze usages. The companion skill `analysis-api-mark-internal-apis` (or a manual review) decides whether to keep
 * the suggested annotation or downgrade to `internal` based on whether the declaration is referenced outside its module.
 *
 * On failure, the first violating file encountered during the filesystem walk is reported via `assertEqualsToFile` with all unmarked
 * declarations annotated using [suggestedAnnotation]. Iterating then surfaces the next file.
 */
abstract class AbstractAnalysisApiInternalApiTest : AbstractAnalysisApiCodebaseValidationTest() {
    /**
     * Returns the marker annotation text (including the leading `@`) to suggest for [declaration]. Called only for declarations that have
     * been determined to be missing a marker.
     */
    protected abstract fun suggestedAnnotation(declaration: KtDeclaration): String

    /**
     * Returns `true` if [declaration] should be exempt from the marker requirement. Override this for declarations that intentionally form
     * a part of the public Analysis API surface despite living in an implementation module (and therefore must remain unmarked).
     *
     * The default returns `false`, treating no declaration as exempt.
     */
    protected open fun isExempt(declaration: KtDeclaration): Boolean = false

    final override fun processFile(file: File, psiFile: PsiFile) {
        if (psiFile !is KtFile) return

        val violations = collectViolations(psiFile)
        if (violations.isEmpty()) return

        val actualText = fileTextWithNewAnnotations(violations)
        if (UPDATE_SOURCE_CODE) {
            file.writeText(actualText)
            fail(
                "Auto-applied ${violations.size} marker annotation(s) to ${file.name}. " +
                        "Re-run the test to surface the next file."
            )
        }
        TestDataAssertions.assertEqualsToFile(buildErrorMessage(violations), file, actualText)
    }

    private fun collectViolations(file: KtFile): List<CodebaseDeclarationAnnotation> {
        val violations = mutableListOf<CodebaseDeclarationAnnotation>()
        for (declaration in file.declarations) {
            collectViolations(declaration, requiredMarkers = emptySet(), violations)
        }
        return violations
    }

    private fun collectViolations(
        declaration: KtDeclaration,
        requiredMarkers: Set<String>,
        result: MutableList<CodebaseDeclarationAnnotation>,
    ) {
        val ownMarkers = INTERNAL_API_MARKER_ANNOTATIONS.filterTo(linkedSetOf()) { declaration.hasAnnotation(it) }

        if (isViolationCandidate(declaration)) {
            // Iterate `INTERNAL_API_MARKER_ANNOTATIONS` so that suggested markers always land in a stable, canonical order regardless of
            // how `requiredMarkers` was constructed by the recursion.
            val missingRequiredMarkers = INTERNAL_API_MARKER_ANNOTATIONS.filterTo(linkedSetOf()) {
                it in requiredMarkers && it !in ownMarkers
            }
            when {
                // The declaration is missing one or more markers that its enclosing classifier(s) require it to repeat. This case takes
                // precedence over `suggestedAnnotation` so the inherited markers always appear, even if the declaration already carries an
                // unrelated marker.
                missingRequiredMarkers.isNotEmpty() -> {
                    missingRequiredMarkers.forEach { result += CodebaseDeclarationAnnotation(declaration, "@$it") }
                }

                // No required markers (e.g. a top-level declaration), and the declaration carries no marker at all.
                ownMarkers.isEmpty() -> {
                    result += CodebaseDeclarationAnnotation(declaration, suggestedAnnotation(declaration))
                }
            }
        }

        // Recurse into nested classifiers regardless of an annotation marker on the parent. The recursion stops only when the parent's
        // visibility (internal/private) already removes it from the public API surface. `protected` does not stop the recursion: a
        // protected nested classifier remains reachable from inheritors in another module.
        if (declaration is KtClassOrObject && declaration.isExternallyVisible) {
            val nestedRequiredMarkers = requiredMarkers + ownMarkers
            for (nested in declaration.declarations.filterIsInstance<KtClassOrObject>()) {
                collectViolations(nested, nestedRequiredMarkers, result)
            }
        }
    }

    private fun isViolationCandidate(declaration: KtDeclaration): Boolean =
        when {
            !declaration.isInScope() -> false
            !declaration.isExternallyVisible -> false
            declaration is KtClass && declaration.isAnnotation() && declaration.hasAnnotation(REQUIRES_OPT_IN) -> false
            isExempt(declaration) -> false
            else -> true
        }

    private fun KtDeclaration.isInScope(): Boolean =
        when (this) {
            // Enum entries are syntactic constituents of the containing enum class. They can't carry their own visibility marker.
            is KtEnumEntry -> false

            is KtClassOrObject -> true
            is KtNamedFunction, is KtProperty, is KtTypeAlias -> this.containingClassOrObject == null
            else -> false
        }

    /**
     * `true` if [this] declaration is reachable from outside its declaring module — that is, `public` (the default) or `protected`. A
     * `protected` nested classifier is still reachable from inheritors in other modules, so it must carry a marker just like a `public`
     * one.
     */
    private val KtDeclaration.isExternallyVisible: Boolean
        get() = isPublic || hasModifier(KtTokens.PROTECTED_KEYWORD)

    private fun buildErrorMessage(violations: List<CodebaseDeclarationAnnotation>): String {
        val count = violations.size
        val declarations = if (count == 1) "declaration" else "declarations"
        val markerAnnotations = INTERNAL_API_MARKER_ANNOTATIONS.joinToString(", ") { "`@$it`" }
        return """
            |Found $count unmarked public $declarations in this file. Public declarations in Analysis API implementation modules must be 
            |marked with `internal`, `private`, or one of: $markerAnnotations.
            |
            |Accepting the suggested change inserts the recommended marker annotation. If a declaration has no callers outside its
            |module, replace the annotation with `internal` instead. (Also see the skill `analysis-api-mark-internal-apis`.)
            """.trimMargin()
    }
}
