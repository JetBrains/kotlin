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
import com.google.common.collect.Lists
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.search.ProjectScope
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.DiagnosticMessageReporter
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.repl.di.ContainerForReplWithJava
import org.jetbrains.kotlin.cli.jvm.repl.di.*
import org.jetbrains.kotlin.cli.jvm.repl.di.ReplLastLineScopeProvider
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
import org.jetbrains.kotlin.context.MutableModuleContext
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
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyScriptDescriptor
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.script.*
import org.jetbrains.kotlin.utils.*

import java.io.File
import java.io.PrintWriter
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections

class ReplInterpreter(
        disposable: Disposable,
        configuration: CompilerConfiguration,
        private val ideMode: Boolean,
        private val replReader: ReplSystemInWrapper?) {

    private var lineNumber = 0

    private var lastLineScope: LexicalScope? = null
    private val earlierLines = Lists.newArrayList<EarlierLine>()
    private val previousIncompleteLines = Lists.newArrayList<String>()
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
                        get() = lastLineScope
                },
                JvmPackagePartProvider(environment))

        this.topDownAnalysisContext = TopDownAnalysisContext(TopDownAnalysisMode.LocalDeclarations, DataFlowInfoFactory.EMPTY,
                                                             container.resolveSession.declarationScopeProvider)
        this.topDownAnalyzer = container.lazyTopDownAnalyzerForTopLevel
        this.resolveSession = container.resolveSession

        moduleContext.initializeModuleContents(CompositePackageFragmentProvider(
                Arrays.asList(
                        container.resolveSession.packageFragmentProvider,
                        container.javaDescriptorResolver.packageFragmentProvider)))

        val classpath = Lists.newArrayList<URL>()
        for (file in configuration.jvmClasspathRoots) {
            try {
                classpath.add(file.toURI().toURL())
            }
            catch (e: MalformedURLException) {
                throw rethrow(e)
            }

        }

        this.classLoader = ReplClassLoader(URLClassLoader(classpath.toArray<URL>(arrayOfNulls<URL>(classpath.size)), null))
    }

    enum class LineResultType {
        SUCCESS,
        COMPILE_ERROR,
        RUNTIME_ERROR,
        INCOMPLETE
    }

    class LineResult private constructor(private val value: Any?, private val unit: Boolean, val errorText: String?, val type: LineResultType) {

        private fun checkSuccessful() {
            if (type != LineResultType.SUCCESS) {
                throw IllegalStateException("it is error")
            }
        }

        fun getValue(): Any? {
            checkSuccessful()
            return value
        }

        val isUnit: Boolean
            get() {
                checkSuccessful()
                return unit
            }

        companion object {

            private fun error(errorText: String, errorType: LineResultType): LineResult {
                var errorText = errorText
                if (errorText.isEmpty()) {
                    errorText = "<unknown error>"
                }
                else if (!errorText.endsWith("\n")) {
                    errorText += "\n"
                }

                return LineResult(null, false, errorText, errorType)
            }

            fun successful(value: Any, unit: Boolean): LineResult {
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

    private fun createDiagnosticHolder(): DiagnosticMessageHolder {
        return if (ideMode)
            ReplIdeDiagnosticMessageHolder()
        else
            ReplTerminalDiagnosticMessageHolder()
    }

    fun eval(line: String): LineResult {
        ++lineNumber

        val scriptFqName = FqName("Line" + lineNumber)

        val fullText = StringBuilder()
        for (prevLine in previousIncompleteLines) {
            fullText.append(prevLine).append("\n")
        }
        fullText.append(line)

        val virtualFile = LightVirtualFile("line" + lineNumber + KotlinParserDefinition.STD_SCRIPT_EXT, KotlinLanguage.INSTANCE, fullText.toString())
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET)
        val psiFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile? ?: error("Script file not analyzed at line $lineNumber: $fullText")

        val errorHolder = createDiagnosticHolder()

        val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(psiFile, errorHolder)

        if (syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof) {
            if (ideMode) {
                return LineResult.compileError(errorHolder.renderedDiagnostics)
            }
            else {
                previousIncompleteLines.add(line)
                return LineResult.incomplete()
            }
        }

        previousIncompleteLines.clear()

        if (syntaxErrorReport.isHasErrors) {
            return LineResult.compileError(errorHolder.renderedDiagnostics)
        }

        prepareForTheNextReplLine(topDownAnalysisContext)
        trace.clearDiagnostics()

        //noinspection ConstantConditions
        psiFile!!.script!!.putUserData(ScriptPriorities.PRIORITY_KEY, lineNumber)

        val scriptDescriptor = doAnalyze(psiFile, errorHolder) ?: return LineResult.compileError(errorHolder.renderedDiagnostics)

        val earlierScripts = Lists.newArrayList<ScriptDescriptor>()

        for (earlierLine in earlierLines) {
            earlierScripts.add(earlierLine.scriptDescriptor)
        }

        val state = GenerationState(psiFile.project, ClassBuilderFactories.BINARIES,
                                    module, trace.bindingContext, listOf(psiFile))

        compileScript(psiFile.script!!, earlierScripts, state, CompilationErrorHandler.THROW_EXCEPTION)

        for (outputFile in state.factory.asList()) {
            if (outputFile.relativePath.endsWith(".class")) {
                classLoader.addClass(JvmClassName.byInternalName(outputFile.relativePath.replaceFirst("\\.class$".toRegex(), "")),
                                     outputFile.asByteArray())
            }
        }

        try {
            val scriptClass = classLoader.loadClass(scriptFqName.asString())

            val constructorParams = arrayOfNulls<Class<*>>(earlierLines.size)
            val constructorArgs = arrayOfNulls<Any>(earlierLines.size)

            for (i in earlierLines.indices) {
                constructorParams[i] = earlierLines[i].scriptClass
                constructorArgs[i] = earlierLines[i].scriptInstance
            }

            val scriptInstanceConstructor = scriptClass.getConstructor(*constructorParams)
            val scriptInstance: Any
            try {
                setReplScriptExecuting(true)
                scriptInstance = scriptInstanceConstructor.newInstance(*constructorArgs)
            }
            catch (e: Throwable) {
                return LineResult.runtimeError(renderStackTrace(e.cause!!))
            }
            finally {
                setReplScriptExecuting(false)
            }

            val rvField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME)
            rvField.isAccessible = true
            val rv = rvField.get(scriptInstance)

            earlierLines.add(EarlierLine(line, scriptDescriptor, scriptClass, scriptInstance))

            return LineResult.successful(rv, !state.replSpecific.hasResult)
        }
        catch (e: Throwable) {
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            val writer = PrintWriter(System.err)
            classLoader.dumpClasses(writer)
            writer.flush()
            throw rethrow(e)
        }

    }

    private fun setReplScriptExecuting(isExecuting: Boolean) {
        if (replReader != null) {
            replReader.isReplScriptExecuting = isExecuting
        }
    }

    private fun doAnalyze(psiFile: KtFile, errorReporter: DiagnosticMessageReporter): ScriptDescriptor? {
        scriptDeclarationFactory.setDelegateFactory(
                FileBasedDeclarationProviderFactory(resolveSession.storageManager, listOf(psiFile)))

        val context = topDownAnalyzer.analyzeDeclarations(
                topDownAnalysisContext.topDownAnalysisMode,
                listOf(psiFile))

        if (trace.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, psiFile) == null) {
            trace.record(BindingContext.FILE_TO_PACKAGE_FRAGMENT, psiFile, resolveSession.getPackageFragment(FqName.ROOT))
        }

        val hasErrors = AnalyzerWithCompilerReport.reportDiagnostics(trace.bindingContext.diagnostics, errorReporter, false)
        if (hasErrors) {
            return null
        }

        val scriptDescriptor = context.scripts[psiFile.script]!!
        lastLineScope = scriptDescriptor.getScopeForInitializerResolution()
        return scriptDescriptor
    }

    fun dumpClasses(out: PrintWriter) {
        classLoader.dumpClasses(out)
    }

    private class ScriptMutableDeclarationProviderFactory : DeclarationProviderFactory {
        private var delegateFactory: DeclarationProviderFactory? = null
        private var rootPackageProvider: AdaptablePackageMemberDeclarationProvider? = null

        fun setDelegateFactory(delegateFactory: DeclarationProviderFactory) {
            this.delegateFactory = delegateFactory

            val provider = delegateFactory.getPackageMemberDeclarationProvider(FqName.ROOT)!!
            if (rootPackageProvider == null) {
                rootPackageProvider = AdaptablePackageMemberDeclarationProvider(provider)
            }
            else {
                rootPackageProvider!!.addDelegateProvider(provider)
            }
        }

        override fun getClassMemberDeclarationProvider(classLikeInfo: KtClassLikeInfo): ClassMemberDeclarationProvider {
            return delegateFactory!!.getClassMemberDeclarationProvider(classLikeInfo)
        }

        override fun getPackageMemberDeclarationProvider(packageFqName: FqName): PackageMemberDeclarationProvider? {
            if (packageFqName.isRoot) {
                return rootPackageProvider
            }

            return this.delegateFactory!!.getPackageMemberDeclarationProvider(packageFqName)
        }

        override fun diagnoseMissingPackageFragment(file: KtFile) {
            this.delegateFactory!!.diagnoseMissingPackageFragment(file)
        }

        class AdaptablePackageMemberDeclarationProvider(private var delegateProvider: PackageMemberDeclarationProvider) : DelegatePackageMemberDeclarationProvider(delegateProvider) {

            fun addDelegateProvider(provider: PackageMemberDeclarationProvider) {
                delegateProvider = CombinedPackageMemberDeclarationProvider(Lists.newArrayList(provider, delegateProvider))

                delegate = delegateProvider
            }
        }
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
        private val REPL_LINE_AS_SCRIPT_DEFINITION = object : KotlinScriptDefinition {
            override fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> {
                return emptyList()
            }

            override fun isScript(file: PsiFile): Boolean {
                return StandardScriptDefinition.isScript(file)
            }

            override fun getScriptName(script: KtScript): Name {
                return StandardScriptDefinition.getScriptName(script)
            }
        }

        private fun prepareForTheNextReplLine(c: TopDownAnalysisContext) {
            c.scripts.clear()
        }

        private fun renderStackTrace(cause: Throwable): String {
            val oldTrace = cause.stackTrace
            val newTrace = ArrayList<StackTraceElement>()
            var skip = true
            for (i in oldTrace.indices.reversed()) {
                val element = oldTrace[i]
                // All our code happens in the script constructor, and no reflection/native code happens in constructors.
                // So we ignore everything in the stack trace until the first constructor
                if (element.getMethodName() == "<init>") {
                    skip = false
                }
                if (!skip) {
                    newTrace.add(element)
                }
            }
            Collections.reverse(newTrace)

            // throw away last element which contains Line1.kts<init>(Unknown source)
            val resultingTrace = newTrace.subList(0, newTrace.size - 1)

            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (cause as java.lang.Throwable).setStackTrace(resultingTrace.toTypedArray())

            return Throwables.getStackTraceAsString(cause)
        }

        fun compileScript(
                script: KtScript,
                earlierScripts: List<ScriptDescriptor>,
                state: GenerationState,
                errorHandler: CompilationErrorHandler) {
            state.replSpecific.scriptResultFieldName = SCRIPT_RESULT_FIELD_NAME
            state.replSpecific.earlierScriptsForReplInterpreter = ArrayList(earlierScripts)

            state.beforeCompile()
            KotlinCodegenFacade.generatePackage(
                    state,
                    script.getContainingKtFile().packageFqName,
                    setOf(script.getContainingKtFile()),
                    errorHandler)
        }
    }
}
