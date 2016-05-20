/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm.repl

import com.google.common.base.Throwables
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.search.ProjectScope
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.DiagnosticMessageReporter
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.getModuleName
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.repl.di.ReplLastLineScopeProvider
import org.jetbrains.kotlin.cli.jvm.repl.di.createContainerForReplWithJava
import org.jetbrains.kotlin.cli.jvm.repl.messages.DiagnosticMessageHolder
import org.jetbrains.kotlin.cli.jvm.repl.messages.ReplIdeDiagnosticMessageHolder
import org.jetbrains.kotlin.cli.jvm.repl.messages.ReplSystemInWrapper
import org.jetbrains.kotlin.cli.jvm.repl.messages.ReplTerminalDiagnosticMessageHolder
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.*
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptParameter
import org.jetbrains.kotlin.script.ScriptPriorities
import org.jetbrains.kotlin.script.StandardScriptDefinition
import java.io.PrintWriter
import java.net.URLClassLoader

class ReplInterpreter(
        disposable: Disposable,
        private val configuration: CompilerConfiguration,
        private val ideMode: Boolean,
        private val replReader: ReplSystemInWrapper?
) {
    private var lineNumber = 0

    private var lastLineScope: LexicalScope? = null
    private val earlierLines = arrayListOf<EarlierLine>()
    private val previousIncompleteLines = arrayListOf<String>()
    private val classLoader: ReplClassLoader

    private val psiFileFactory: PsiFileFactoryImpl
    private val trace: BindingTraceContext
    private val module: ModuleDescriptorImpl

    private val topDownAnalysisContext: TopDownAnalysisContext
    private val topDownAnalyzer: LazyTopDownAnalyzerForTopLevel
    private val resolveSession: ResolveSession
    private val scriptDeclarationFactory: ScriptMutableDeclarationProviderFactory

    init {
        configuration.add(CommonConfigurationKeys.SCRIPT_DEFINITIONS_KEY, REPL_LINE_AS_SCRIPT_DEFINITION)

        val environment = KotlinCoreEnvironment.createForProduction(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val project = environment.project

        this.psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
        this.trace = CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace()
        val moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(project, environment.getModuleName())
        this.module = moduleContext.module

        scriptDeclarationFactory = ScriptMutableDeclarationProviderFactory()

        val container = createContainerForReplWithJava(
                moduleContext,
                trace,
                scriptDeclarationFactory,
                ProjectScope.getAllScope(project),
                object : ReplLastLineScopeProvider {
                    override val lastLineScope: LexicalScope?
                        get() = this@ReplInterpreter.lastLineScope
                },
                JvmPackagePartProvider(environment)
        )

        this.topDownAnalysisContext = TopDownAnalysisContext(
                TopDownAnalysisMode.LocalDeclarations, DataFlowInfoFactory.EMPTY, container.resolveSession.declarationScopeProvider
        )
        this.topDownAnalyzer = container.lazyTopDownAnalyzerForTopLevel
        this.resolveSession = container.resolveSession

        moduleContext.initializeModuleContents(CompositePackageFragmentProvider(
                listOf(
                        container.resolveSession.packageFragmentProvider,
                        container.javaDescriptorResolver.packageFragmentProvider
                )
        ))

        val classpath = configuration.jvmClasspathRoots.map { it.toURI().toURL() }

        this.classLoader = ReplClassLoader(URLClassLoader(classpath.toTypedArray(), null))
    }

    enum class LineResultType {
        SUCCESS,
        COMPILE_ERROR,
        RUNTIME_ERROR,
        INCOMPLETE
    }

    class LineResult private constructor(
            private val resultingValue: Any?,
            private val unit: Boolean,
            val errorText: String?,
            val type: LineResultType
    ) {
        val value: Any?
            get() {
                checkSuccessful()
                return resultingValue
            }

        val isUnit: Boolean
            get() {
                checkSuccessful()
                return unit
            }

        private fun checkSuccessful() {
            if (type != LineResultType.SUCCESS) {
                error("it is error")
            }
        }

        companion object {
            private fun error(errorText: String, errorType: LineResultType): LineResult {
                val resultingErrorText = when {
                    errorText.isEmpty() -> "<unknown error>"
                    !errorText.endsWith("\n") -> errorText + "\n"
                    else -> errorText
                }

                return LineResult(null, false, resultingErrorText, errorType)
            }

            fun successful(value: Any?, unit: Boolean): LineResult {
                return LineResult(value, unit, null, LineResultType.SUCCESS)
            }

            fun compileError(errorText: String): LineResult {
                return error(errorText, LineResultType.COMPILE_ERROR)
            }

            fun runtimeError(errorText: String): LineResult {
                return error(errorText, LineResultType.RUNTIME_ERROR)
            }

            fun incomplete(): LineResult {
                return LineResult(null, false, null, LineResultType.INCOMPLETE)
            }
        }
    }

    private fun createDiagnosticHolder(): DiagnosticMessageHolder =
            if (ideMode)
                ReplIdeDiagnosticMessageHolder()
            else
                ReplTerminalDiagnosticMessageHolder()

    fun eval(line: String): LineResult {
        ++lineNumber

        val fullText = (previousIncompleteLines + line).joinToString(separator = "\n")

        val virtualFile =
                LightVirtualFile("line$lineNumber${KotlinParserDefinition.STD_SCRIPT_EXT}", KotlinLanguage.INSTANCE, fullText).apply {
                    charset = CharsetToolkit.UTF8_CHARSET
                }
        val psiFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
                      ?: error("Script file not analyzed at line $lineNumber: $fullText")

        val errorHolder = createDiagnosticHolder()

        val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(psiFile, errorHolder)

        if (syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof) {
            return if (ideMode) {
                LineResult.compileError(errorHolder.renderedDiagnostics)
            }
            else {
                previousIncompleteLines.add(line)
                LineResult.incomplete()
            }
        }

        previousIncompleteLines.clear()

        if (syntaxErrorReport.isHasErrors) {
            return LineResult.compileError(errorHolder.renderedDiagnostics)
        }

        prepareForTheNextReplLine(topDownAnalysisContext)
        trace.clearDiagnostics()

        psiFile.script!!.putUserData(ScriptPriorities.PRIORITY_KEY, lineNumber)

        val scriptDescriptor = doAnalyze(psiFile, errorHolder)
                               ?: return LineResult.compileError(errorHolder.renderedDiagnostics)

        val state = GenerationState(
                psiFile.project, ClassBuilderFactories.BINARIES, module, trace.bindingContext, listOf(psiFile), configuration
        )

        compileScript(psiFile.script!!, earlierLines.map(EarlierLine::getScriptDescriptor), state, CompilationErrorHandler.THROW_EXCEPTION)

        for (outputFile in state.factory.asList()) {
            if (outputFile.relativePath.endsWith(".class")) {
                classLoader.addClass(JvmClassName.byInternalName(outputFile.relativePath.replaceFirst("\\.class$".toRegex(), "")),
                                     outputFile.asByteArray())
            }
        }

        try {
            val scriptClass = classLoader.loadClass("Line$lineNumber")

            val constructorParams = earlierLines.map(EarlierLine::getScriptClass).toTypedArray()
            val constructorArgs = earlierLines.map(EarlierLine::getScriptInstance).toTypedArray()

            val scriptInstanceConstructor = scriptClass.getConstructor(*constructorParams)
            val scriptInstance = try {
                replReader?.isReplScriptExecuting = true
                scriptInstanceConstructor.newInstance(*constructorArgs)
            }
            catch (e: Throwable) {
                return LineResult.runtimeError(renderStackTrace(e.cause!!))
            }
            finally {
                replReader?.isReplScriptExecuting = false
            }

            val rvField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME).apply { isAccessible = true }
            val rv = rvField.get(scriptInstance)

            earlierLines.add(EarlierLine(line, scriptDescriptor, scriptClass, scriptInstance))

            return LineResult.successful(rv, !state.replSpecific.hasResult)
        }
        catch (e: Throwable) {
            val writer = PrintWriter(System.err)
            classLoader.dumpClasses(writer)
            writer.flush()
            throw e
        }
    }

    private fun doAnalyze(psiFile: KtFile, errorReporter: DiagnosticMessageReporter): ScriptDescriptor? {
        scriptDeclarationFactory.setDelegateFactory(FileBasedDeclarationProviderFactory(resolveSession.storageManager, listOf(psiFile)))

        val context = topDownAnalyzer.analyzeDeclarations(topDownAnalysisContext.topDownAnalysisMode, listOf(psiFile))

        if (trace.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, psiFile) == null) {
            trace.record(BindingContext.FILE_TO_PACKAGE_FRAGMENT, psiFile, resolveSession.getPackageFragment(FqName.ROOT))
        }

        val hasErrors = AnalyzerWithCompilerReport.reportDiagnostics(trace.bindingContext.diagnostics, errorReporter, false)
        if (hasErrors) {
            return null
        }

        return context.scripts[psiFile.script]?.apply {
            lastLineScope = this.scopeForInitializerResolution
        }
    }

    fun dumpClasses(out: PrintWriter) {
        classLoader.dumpClasses(out)
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

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
        private val REPL_LINE_AS_SCRIPT_DEFINITION = object : KotlinScriptDefinition {
            override fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> = emptyList()

            override fun isScript(file: PsiFile): Boolean = StandardScriptDefinition.isScript(file)

            override fun getScriptName(script: KtScript): Name = StandardScriptDefinition.getScriptName(script)
        }

        private fun prepareForTheNextReplLine(c: TopDownAnalysisContext) {
            c.scripts.clear()
        }

        private fun renderStackTrace(cause: Throwable): String {
            val newTrace = arrayListOf<StackTraceElement>()
            var skip = true
            for ((i, element) in cause.stackTrace.withIndex().reversed()) {
                // All our code happens in the script constructor, and no reflection/native code happens in constructors.
                // So we ignore everything in the stack trace until the first constructor
                if (element.methodName == "<init>") {
                    skip = false
                }
                if (!skip) {
                    newTrace.add(element)
                }
            }

            // throw away last element which contains Line1.kts<init>(Unknown source)
            val resultingTrace = newTrace.reversed().dropLast(1)

            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UsePropertyAccessSyntax")
            (cause as java.lang.Throwable).setStackTrace(resultingTrace.toTypedArray())

            return Throwables.getStackTraceAsString(cause)
        }

        fun compileScript(
                script: KtScript,
                earlierScripts: List<ScriptDescriptor>,
                state: GenerationState,
                errorHandler: CompilationErrorHandler
        ) {
            state.replSpecific.scriptResultFieldName = SCRIPT_RESULT_FIELD_NAME
            state.replSpecific.earlierScriptsForReplInterpreter = earlierScripts.toList()

            state.beforeCompile()
            KotlinCodegenFacade.generatePackage(
                    state,
                    script.getContainingKtFile().packageFqName,
                    setOf(script.getContainingKtFile()),
                    errorHandler
            )
        }
    }
}
