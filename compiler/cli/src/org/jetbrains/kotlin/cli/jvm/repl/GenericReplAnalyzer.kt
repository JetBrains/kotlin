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

@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.kotlin.cli.jvm.repl

import org.jetbrains.kotlin.cli.common.repl.CompiledReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplHistory
import org.jetbrains.kotlin.cli.common.repl.ReplResettableCodeLine
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyScriptDescriptor
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.utils.replaceImportingScopes
import org.jetbrains.kotlin.script.ScriptPriorities
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class GenericReplAnalyzer(environment: KotlinCoreEnvironment,
                          protected val stateLock: ReentrantReadWriteLock = ReentrantReadWriteLock()) : ReplResettableCodeLine {
    private val topDownAnalysisContext: TopDownAnalysisContext
    private val topDownAnalyzer: LazyTopDownAnalyzer
    private val resolveSession: ResolveSession
    private val scriptDeclarationFactory: ScriptMutableDeclarationProviderFactory
    private val replState = ResetableReplState()

    val module: ModuleDescriptorImpl
        get() = stateLock.read { field }

    val trace: BindingTraceContext = CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace()
        get() = stateLock.read { field }

    init {
        // Module source scope is empty because all binary classes are in the dependency module, and all source classes are guaranteed
        // to be found via ResolveSession. The latter is true as long as light classes are not needed in NONE (which is currently true
        // because no symbol declared in the NONE session can be used from Java)
        val container = TopDownAnalyzerFacadeForJVM.createContainer(
                environment.project,
                emptyList(),
                trace,
                environment.configuration,
                { scope -> JvmPackagePartProvider(environment, scope) },
                { storageManager, files -> ScriptMutableDeclarationProviderFactory() }
        )

        this.module = container.get<ModuleDescriptorImpl>()
        this.scriptDeclarationFactory = container.get<ScriptMutableDeclarationProviderFactory>()
        this.resolveSession = container.get<ResolveSession>()
        this.topDownAnalysisContext = TopDownAnalysisContext(
                TopDownAnalysisMode.LocalDeclarations, DataFlowInfoFactory.EMPTY, resolveSession.declarationScopeProvider
        )
        this.topDownAnalyzer = container.get<LazyTopDownAnalyzer>()
    }

    interface ReplLineAnalysisResult {
        val scriptDescriptor: ScriptDescriptor?
        val diagnostics: Diagnostics

        data class Successful(override val scriptDescriptor: ScriptDescriptor, override val diagnostics: Diagnostics) : ReplLineAnalysisResult

        data class WithErrors(override val diagnostics: Diagnostics) : ReplLineAnalysisResult {
            override val scriptDescriptor: ScriptDescriptor? get() = null
        }
    }

    override fun resetToLine(lineNumber: Int): List<ReplCodeLine> {
        return stateLock.write { replState.resetToLine(lineNumber) }
    }

    override fun resetToLine(line: ReplCodeLine): List<ReplCodeLine> = resetToLine(line.no)

    fun analyzeReplLine(psiFile: KtFile, codeLine: ReplCodeLine): ReplLineAnalysisResult {
        return stateLock.write {
            topDownAnalysisContext.scripts.clear()
            trace.clearDiagnostics()

            psiFile.script!!.putUserData(ScriptPriorities.PRIORITY_KEY, codeLine.no)

            doAnalyze(psiFile, codeLine)
        }
    }

    private fun doAnalyze(linePsi: KtFile, codeLine: ReplCodeLine): ReplLineAnalysisResult {
        scriptDeclarationFactory.setDelegateFactory(FileBasedDeclarationProviderFactory(resolveSession.storageManager, listOf(linePsi)))
        replState.submitLine(linePsi, codeLine)

        val context = topDownAnalyzer.analyzeDeclarations(topDownAnalysisContext.topDownAnalysisMode, listOf(linePsi))

        if (trace.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, linePsi) == null) {
            trace.record(BindingContext.FILE_TO_PACKAGE_FRAGMENT, linePsi, resolveSession.getPackageFragment(FqName.ROOT))
        }

        val diagnostics = trace.bindingContext.diagnostics
        val hasErrors = diagnostics.any { it.severity == Severity.ERROR }
        if (hasErrors) {
            replState.lineFailure(linePsi, codeLine)
            return ReplLineAnalysisResult.WithErrors(diagnostics)
        }
        else {
            val scriptDescriptor = context.scripts[linePsi.script]!!
            replState.lineSuccess(linePsi, codeLine, scriptDescriptor)
            return ReplLineAnalysisResult.Successful(scriptDescriptor, diagnostics)
        }

    }

    private class ScriptMutableDeclarationProviderFactory : DeclarationProviderFactory {
        private lateinit var delegateFactory: DeclarationProviderFactory
        private lateinit var rootPackageProvider: AdaptablePackageMemberDeclarationProvider

        fun setDelegateFactory(delegateFactory: DeclarationProviderFactory) {
            this.delegateFactory = delegateFactory

            val provider = delegateFactory.getPackageMemberDeclarationProvider(FqName.ROOT)!!
            try {
                rootPackageProvider.addDelegateProvider(provider)
            }
            catch (e: UninitializedPropertyAccessException) {
                rootPackageProvider = AdaptablePackageMemberDeclarationProvider(provider)
            }
        }

        override fun getClassMemberDeclarationProvider(classLikeInfo: KtClassLikeInfo): ClassMemberDeclarationProvider {
            return delegateFactory.getClassMemberDeclarationProvider(classLikeInfo)
        }

        override fun getPackageMemberDeclarationProvider(packageFqName: FqName): PackageMemberDeclarationProvider? {
            if (packageFqName.isRoot) {
                return rootPackageProvider
            }

            return delegateFactory.getPackageMemberDeclarationProvider(packageFqName)
        }

        override fun diagnoseMissingPackageFragment(file: KtFile) {
            delegateFactory.diagnoseMissingPackageFragment(file)
        }

        class AdaptablePackageMemberDeclarationProvider(
                private var delegateProvider: PackageMemberDeclarationProvider
        ) : DelegatePackageMemberDeclarationProvider(delegateProvider) {
            fun addDelegateProvider(provider: PackageMemberDeclarationProvider) {
                delegateProvider = CombinedPackageMemberDeclarationProvider(listOf(provider, delegateProvider))

                delegate = delegateProvider
            }
        }
    }

    class ResetableReplState() {
        private val successfulLines = ReplHistory<LineInfo.SuccessfulLine>()
        private val submittedLines = hashMapOf<KtFile, LineInfo>()

        fun resetToLine(lineNumber: Int): List<ReplCodeLine> {
            val removed = successfulLines.resetToLine(lineNumber)
            removed.forEach { submittedLines.remove(it.second.linePsi) }
            return removed.map { it.first }
        }

        fun resetToLine(line: ReplCodeLine): List<ReplCodeLine> = resetToLine(line.no)

        fun submitLine(ktFile: KtFile, codeLine: ReplCodeLine) {
            val line = LineInfo.SubmittedLine(ktFile, successfulLines.lastValue())
            submittedLines[ktFile] = line
            ktFile.fileScopesCustomizer = object : FileScopesCustomizer {
                override fun createFileScopes(fileScopeFactory: FileScopeFactory): FileScopes {
                    return lineInfo(ktFile)?.let { computeFileScopes(it, fileScopeFactory) } ?: fileScopeFactory.createScopesForFile(ktFile)
                }
            }
        }

        fun lineSuccess(ktFile: KtFile, codeLine: ReplCodeLine, scriptDescriptor: LazyScriptDescriptor) {
            val successfulLine = LineInfo.SuccessfulLine(ktFile, successfulLines.lastValue(), scriptDescriptor)
            submittedLines[ktFile] = successfulLine
            successfulLines.add(CompiledReplCodeLine(ktFile.name, codeLine), successfulLine)
        }

        fun lineFailure(ktFile: KtFile, codeLine: ReplCodeLine) {
            submittedLines[ktFile] = LineInfo.FailedLine(ktFile, successfulLines.lastValue())
        }

        private fun lineInfo(ktFile: KtFile) = submittedLines[ktFile]

        // use sealed?
        private sealed class LineInfo {
            abstract val linePsi: KtFile
            abstract val parentLine: SuccessfulLine?

            class SubmittedLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?) : LineInfo()
            class SuccessfulLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?, val lineDescriptor: LazyScriptDescriptor) : LineInfo()
            class FailedLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?) : LineInfo()
        }

        private fun computeFileScopes(lineInfo: LineInfo, fileScopeFactory: FileScopeFactory): FileScopes? {
            // create scope that wraps previous line lexical scope and adds imports from this line
            val lexicalScopeAfterLastLine = lineInfo.parentLine?.lineDescriptor?.scopeForInitializerResolution ?: return null
            val lastLineImports = lexicalScopeAfterLastLine.parentsWithSelf.first { it is ImportingScope } as ImportingScope
            val scopesForThisLine = fileScopeFactory.createScopesForFile(lineInfo.linePsi, lastLineImports)
            val combinedLexicalScopes = lexicalScopeAfterLastLine.replaceImportingScopes(scopesForThisLine.importingScope)
            return FileScopes(combinedLexicalScopes, scopesForThisLine.importingScope, scopesForThisLine.importResolver)
        }
    }
}


