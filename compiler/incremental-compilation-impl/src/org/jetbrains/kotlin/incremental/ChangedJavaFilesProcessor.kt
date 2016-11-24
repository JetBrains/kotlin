/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.incremental

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.lang.java.JavaLanguage
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import java.io.File
import java.util.*

internal class ChangedJavaFilesProcessor(private val reporter: ICReporter) {
    private val allSymbols = HashSet<LookupSymbol>()
    private val javaLang = JavaLanguage.INSTANCE
    private val psiFileFactory: PsiFileFactory by lazy {
        val rootDisposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration()
        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val project = environment.project
        PsiFileFactory.getInstance(project)
    }

    val allChangedSymbols: Collection<LookupSymbol>
            get() = allSymbols

    fun process(filesDiff: ChangedFiles.Known): ChangesEither {
        val modifiedJava = filesDiff.modified.filter(File::isJavaFile)
        val removedJava = filesDiff.removed.filter(File::isJavaFile)

        if (removedJava.any()) {
            reporter.report { "Some java files are removed: [${removedJava.joinToString()}]" }
            return ChangesEither.Unknown()
        }

        val symbols = HashSet<LookupSymbol>()
        for (javaFile in modifiedJava) {
            assert(javaFile.extension.equals("java", ignoreCase = true))

            val psiFile = javaFile.psiFile()
            if (psiFile !is PsiJavaFile) {
                reporter.report { "Expected PsiJavaFile, got ${psiFile?.javaClass}" }
                return ChangesEither.Unknown()
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

    private fun File.psiFile(): PsiFile? =
            psiFileFactory.createFileFromText(nameWithoutExtension, javaLang, readText())
}