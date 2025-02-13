/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.javaInterop

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.ChangesEither
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.isJavaFile
import java.io.File

internal class ChangedJavaFilesProcessor(
    private val reporter: ICReporter,
    private val psiFileFactory: (File) -> PsiFile?
) {
    private val allSymbols = HashSet<LookupSymbol>()

    val allChangedSymbols: Collection<LookupSymbol>
        get() = allSymbols

    fun process(filesDiff: ChangedFiles.DeterminableFiles.Known): ChangesEither {
        val modifiedJava = filesDiff.modified.filter(File::isJavaFile)
        val removedJava = filesDiff.removed.filter(File::isJavaFile)

        if (removedJava.any()) {
            reporter.info { "Some java files are removed: [${removedJava.joinToString()}]" }
            return ChangesEither.Unknown(BuildAttribute.JAVA_CHANGE_UNTRACKED_FILE_IS_REMOVED)
        }

        val symbols = HashSet<LookupSymbol>()
        for (javaFile in modifiedJava) {
            assert(javaFile.extension.equals("java", ignoreCase = true))

            val psiFile = psiFileFactory(javaFile)
            if (psiFile !is PsiJavaFile) {
                reporter.info { "Expected PsiJavaFile, got ${psiFile?.javaClass}" }
                return ChangesEither.Unknown(BuildAttribute.JAVA_CHANGE_UNEXPECTED_PSI)
            }

            psiFile.classes.forEach { it.addLookupSymbols(symbols) }
        }
        allSymbols.addAll(symbols)
        return ChangesEither.Known(lookupSymbols = symbols)
    }

    private fun PsiClass.addLookupSymbols(symbols: MutableSet<LookupSymbol>) {
        val fqn = qualifiedName.orEmpty()

        symbols.add(LookupSymbol(name.orEmpty(), if (fqn == name) "" else fqn.removeSuffix("." + name!!)))
        methods.forEach { symbols.add(LookupSymbol(it.name, fqn)) }
        fields.forEach { symbols.add(LookupSymbol(it.name.orEmpty(), fqn)) }
        innerClasses.forEach { it.addLookupSymbols(symbols) }
    }
}
