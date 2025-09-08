/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils.offsetToLineAndColumn
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives

inline fun <T> runReadAction(crossinline runnable: () -> T): T {
    return ApplicationManager.getApplication().runReadAction(Computable { runnable() })
}

fun <R> executeOnPooledThreadInReadAction(action: () -> R): R =
    ApplicationManager.getApplication().executeOnPooledThread<R> { runReadAction(action) }.get()

fun PsiElement?.position(): String {
    if (this == null) return "(unknown)"
    return offsetToLineAndColumn(containingFile.viewProvider.document, textRange.startOffset).toString()
}

fun KaSymbol.getNameWithPositionString(): String {
    return when (val psi = this.psi) {
        is KtDeclarationWithBody -> psi.name
        is KtNamedDeclaration -> psi.name
        null -> "null"
        else -> psi::class.simpleName
    } + "@" + psi.position()
}

fun String.indented(indent: Int): String {
    val indentString = " ".repeat(indent)
    return indentString + replace("\n", "\n$indentString")
}

fun KtDeclaration.getNameWithPositionString(): String {
    return (presentation?.presentableText ?: name ?: this::class.simpleName) + "@" + position()
}

/**
 * Renders a description of where the [KtElement] is located, which may be used in test results.
 *
 * In contrast to [position], [renderLocationDescription] includes the file name from which the [KtElement] originates and supports compiled
 * files.
 *
 * #### Examples
 *
 * ```
 * 'main.kt' (127,165)              // An element in a file 'main.kt' with the position (127, 165).
 * 'library1.jar!/library/A.class'  // An element in a class file 'A.class' in the JAR `library1.jar`.
 * ```
 */
fun KtElement.renderLocationDescription(): String {
    // Fallback builtins are sourced from the test's runtime. We shouldn't use the containing JAR file as a location description, since it
    // might change based on the test environment.
    val module = KotlinProjectStructureProvider.getModule(project, this@renderLocationDescription, useSiteModule = null)
    if (module is KaBuiltinsModule) {
        return "'fallback builtins'"
    }

    val ktFile = containingKtFile
    val virtualFile = ktFile.virtualFile
    val fileSystem = virtualFile.fileSystem
    val fileDescription = if (fileSystem is CoreJarFileSystem) {
        virtualFile.path.split("/").dropWhile { !it.endsWith(".jar!") }.joinToString("/").takeIf { it.isNotEmpty() }
            ?: error("Expected a JAR file path for a virtual file in a JAR file system: ${virtualFile.path}")
    } else {
        virtualFile.name
    }.stripOutSnapshotVersion()

    return buildString {
        append("'$fileDescription'")

        if (!ktFile.isCompiled) {
            append(" ${position()}")
        }
    }
}

fun findReferencesAtCaret(mainKtFile: KtFile, caretPosition: Int): List<KtReference> =
    mainKtFile.findReferenceAt(caretPosition)?.unwrapMultiReferences().orEmpty().filterIsInstance<KtReference>()

fun PsiReference.unwrapMultiReferences(): List<PsiReference> = when (this) {
    is KtReference -> listOf(this)
    is PsiMultiReference -> references.flatMap { it.unwrapMultiReferences() }
    else -> error("Unexpected reference $this")
}

/**
 * [AfterAnalysisChecker][org.jetbrains.kotlin.test.model.AfterAnalysisChecker] should be the preferred option
 */
fun RegisteredDirectives.ignoreExceptionIfIgnoreDirectivePresent(ignoreDirective: Directive, action: () -> Unit) {
    var exception: Throwable? = null
    try {
        action()
    } catch (e: Throwable) {
        exception = e
    }

    if (ignoreDirective in this) {
        if (exception != null) return
        error("$ignoreDirective is redundant")
    }

    if (exception != null) {
        throw exception
    }
}

/**
 * Transforms [this] collection with [transformer] and return single or null value. Throws [error] in the case of more than one element.
 */
fun <T, R> Collection<T>.singleOrZeroValue(
    transformer: (T) -> R?,
    ambiguityValueRenderer: (R) -> String,
): R? {
    val newCollection = mapNotNull(transformer)
    return when (newCollection.size) {
        0 -> null
        1 -> newCollection.single()
        else -> error(buildString {
            appendLine("Ambiguity values are not expected.")
            newCollection.joinTo(this, separator = "\n", transform = ambiguityValueRenderer)
        })
    }
}

private val snapshotVersionRegex: Regex = """-2\.\d\.\d+-(\w+-\d+|SNAPSHOT)""".toRegex()

/**
 * Removes version suffix from kotlin libraries, like stdlib or kotlin-reflect:
 * kotlin-stdlib-2.3.255-SNAPSHOT -> kotlin-stdlib
 * kotlin-stdlib-2.3.0-dev-1234 -> kotlin-stdlib
 */
fun String.stripOutSnapshotVersion(): String = replace(snapshotVersionRegex, "")
