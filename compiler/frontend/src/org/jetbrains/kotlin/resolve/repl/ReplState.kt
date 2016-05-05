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

package org.jetbrains.kotlin.resolve.repl

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.FileScopeFactory
import org.jetbrains.kotlin.resolve.lazy.FileScopes
import org.jetbrains.kotlin.resolve.lazy.FileScopesCustomizer
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyScriptDescriptor
import org.jetbrains.kotlin.resolve.lazy.fileScopesCustomizer
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.utils.replaceImportingScopes
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class ReplState {
    private val lines = hashMapOf<KtFile, LineInfo>()
    private val successfulLines = arrayListOf<SuccessfulLine>()

    val successfulLinesCount: Int
        get() = successfulLines.size

    fun submitLine(ktFile: KtFile) {
        val line = SubmittedLine(ktFile, successfulLines.lastOrNull())
        lines[ktFile] = line
        ktFile.fileScopesCustomizer = object : FileScopesCustomizer {
            override fun createFileScopes(fileScopeFactory: FileScopeFactory): FileScopes {
                return lineInfo(ktFile)?.computeFileScopes(fileScopeFactory) ?: fileScopeFactory.createScopesForFile(ktFile)
            }
        }
    }

    fun lineSuccess(ktFile: KtFile, scriptDescriptor: LazyScriptDescriptor) {
        val successfulLine = SuccessfulLine(ktFile, successfulLines.lastOrNull(), scriptDescriptor)
        lines[ktFile] = successfulLine
        successfulLines.add(successfulLine)
    }

    fun lineFailure(ktFile: KtFile) {
        lines[ktFile] = FailedLine(ktFile, successfulLines.lastOrNull())
    }

    fun lineInfo(ktFile: KtFile) = lines[ktFile]

    inner abstract class LineInfo {
        abstract val linePsi: KtFile
        abstract val parentLine: SuccessfulLine?

        fun computeFileScopes(fileScopeFactory: FileScopeFactory): FileScopes {
            // create scope that wraps previous line lexical scope and adds imports from this line
            val lexicalScopeAfterLastLine = parentLine?.lineDescriptor?.scopeForInitializerResolution
                                            ?: return fileScopeFactory.createScopesForFile(linePsi)
            val lastLineImports = lexicalScopeAfterLastLine.parentsWithSelf.firstIsInstance<ImportingScope>()
            val scopesForThisLine = fileScopeFactory.createScopesForFile(linePsi, lastLineImports)
            val combinedLexicalScopes = lexicalScopeAfterLastLine.replaceImportingScopes(scopesForThisLine.importingScope)
            return FileScopes(combinedLexicalScopes, scopesForThisLine.importingScope, scopesForThisLine.importResolver)
        }
    }

    inner class SubmittedLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?) : LineInfo()
    inner class SuccessfulLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?, val lineDescriptor: LazyScriptDescriptor) : LineInfo()
    inner class FailedLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?) : LineInfo()
}
