/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility

import com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.parentClassId
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.BytecodeListingTextCollectingVisitor
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.plugin.sandbox.PluginRuntimeAnnotationsProvider
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarFile
import kotlin.reflect.jvm.jvmName

abstract class AbstractFirPluginPrototypeMultiModuleCompilerFacilityTest : AbstractCompilerFacilityTest() {
    override fun extraCustomRuntimeClasspathProviders(): Array<Constructor<RuntimeClasspathProvider>> =
        arrayOf(::PluginRuntimeAnnotationsProvider)
}

abstract class AbstractCompilerFacilityTest : AbstractAnalysisApiBasedTest() {
    private companion object {
        private val ALLOWED_ERRORS = listOf(
            FirErrors.INVISIBLE_REFERENCE,
            FirErrors.INVISIBLE_SETTER,
            FirErrors.DEPRECATION_ERROR,
            FirErrors.DIVISION_BY_ZERO,
            FirErrors.OPT_IN_USAGE_ERROR,
            FirErrors.OPT_IN_TO_INHERITANCE_ERROR,
            FirErrors.OPT_IN_OVERRIDE_ERROR,
            FirErrors.UNSAFE_CALL,
            FirErrors.UNSAFE_IMPLICIT_INVOKE_CALL,
            FirErrors.UNSAFE_INFIX_CALL,
            FirErrors.UNSAFE_OPERATOR_CALL,
            FirErrors.ITERATOR_ON_NULLABLE,
            FirErrors.UNEXPECTED_SAFE_CALL,
            FirErrors.DSL_SCOPE_VIOLATION,
        ).map { it.name }
    }

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        if (mainFile == null) {
            assert(mainModule.moduleKind == TestModuleKind.LibraryBinary)

            val binaryMainModule = mainModule.ktModule as KaLibraryModule
            val binaryMainModuleAsFile =
                binaryMainModule.binaryRoots.singleOrNull()?.toFile() ?: error("The binary main module must have a single Jar file")
            val actualText = dumpClassesFromJar(binaryMainModuleAsFile)
            testServices.assertions.assertEqualsToTestDataFileSibling(actualText)
            return
        }
        super.doTestByMainModuleAndOptionalMainFile(mainFile, mainModule, testServices)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val testFile = mainModule.testModule.files.single { it.name == mainFile.name }

        val annotationToCheckCalls = mainModule.testModule.directives[Directives.CHECK_CALLS_WITH_ANNOTATION].singleOrNull()
        val irCollector = CollectingIrGenerationExtension(annotationToCheckCalls)

        val project = mainFile.project
        project.extensionArea.getExtensionPoint(IrGenerationExtension.extensionPointName)
            .registerExtension(irCollector, LoadingOrder.LAST, project)

        val compilerConfiguration = CompilerConfiguration().apply {
            put(CommonConfigurationKeys.MODULE_NAME, mainModule.testModule.name)
            put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, mainModule.testModule.languageVersionSettings)
            put(JVMConfigurationKeys.IR, true)

            testFile.directives[Directives.CODE_FRAGMENT_CLASS_NAME].singleOrNull()
                ?.let { put(KaCompilerFacility.CODE_FRAGMENT_CLASS_NAME, it) }

            testFile.directives[Directives.CODE_FRAGMENT_METHOD_NAME].singleOrNull()
                ?.let { put(KaCompilerFacility.CODE_FRAGMENT_METHOD_NAME, it) }
        }

        analyze(mainFile) {
            val target = KaCompilerTarget.Jvm(isTestMode = true)
            val allowedErrorFilter: (KaDiagnostic) -> Boolean = { it.factoryName in ALLOWED_ERRORS }

            val exceptionExpected = mainModule.testModule.directives.contains(Directives.CODE_COMPILATION_EXCEPTION)
            val result = try {
                compile(mainFile, compilerConfiguration, target, allowedErrorFilter)
            } catch (e: Throwable) {
                if (exceptionExpected && e is KaCodeCompilationException) {
                    e.cause?.message?.let { testServices.assertions.assertEqualsToTestDataFileSibling("CODE_COMPILATION_EXCEPTION:\n$it") }
                        ?: throw e
                    return
                }
                throw e
            }

            val actualText = when (result) {
                is KaCompilationResult.Failure -> result.errors.joinToString("\n") { dumpDiagnostic(it) }
                is KaCompilationResult.Success -> dumpClassFiles(result.output)
            }

            testServices.assertions.assertEqualsToTestDataFileSibling(actualText)

            if (result is KaCompilationResult.Success) {
                testServices.assertions.assertEqualsToTestDataFileSibling(irCollector.result, extension = ".ir.txt")
            }

            if (annotationToCheckCalls != null) {
                testServices.assertions.assertEqualsToTestDataFileSibling(
                    irCollector.functionsWithAnnotationToCheckCalls.joinToString("\n"), extension = ".check_calls.txt"
                )
            }
        }
    }

    open fun extraCustomRuntimeClasspathProviders(): Array<Constructor<RuntimeClasspathProvider>> = emptyArray()

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(Directives)
            useConfigurators(::CompilerFacilityEnvironmentConfigurator)
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
                +JvmEnvironmentConfigurationDirectives.FULL_JDK
            }
            useCustomRuntimeClasspathProviders(*extraCustomRuntimeClasspathProviders())
        }
    }

    private fun dumpDiagnostic(diagnostic: KaDiagnostic): String {
        val textRanges = when (diagnostic) {
            is KaDiagnosticWithPsi<*> -> {
                diagnostic.textRanges.singleOrNull()?.toString()
                    ?: diagnostic.textRanges.joinToString(prefix = "[", postfix = "]")
            }
            else -> null
        }

        return buildString {
            if (textRanges != null) {
                append(textRanges)
                append(" ")
            }
            append(diagnostic.factoryName)
            append(" ")
            append(diagnostic.defaultMessage)
        }
    }

    private fun dumpClassFiles(outputFiles: List<KaCompiledFile>): String {
        val classReaders =
            outputFiles.filter { it.path.endsWith(".class", ignoreCase = true) }.also { check(it.isNotEmpty()) }.sortedBy { it.path }
                .map { ClassReader(it.content) }
        return dumpClassFromClassReaders(classReaders)
    }

    private fun dumpClassesFromJar(jar: File): String {
        val jarFile = JarFile(jar)
        val entries = jarFile.entries()
        val classInputStreamList = buildList {
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.endsWith(".class")) {
                    add(entry)
                }
            }
        }.sortedBy { it.name }.map { jarFile.getInputStream(it) }

        val result = dumpClassFromClassReaders(classInputStreamList.map { ClassReader(it) })
        classInputStreamList.forEach { it.close() }
        jarFile.close()

        return result
    }

    private fun dumpClassFromClassReaders(classReaders: List<ClassReader>): String {
        val classes = classReaders.map { classReader ->
            ClassNode(Opcodes.API_VERSION).also { classReader.accept(it, ClassReader.SKIP_CODE) }
        }

        val allClasses = classes.associateBy { Type.getObjectType(it.name) }

        return classes.joinToString("\n\n") { node ->
            val visitor = BytecodeListingTextCollectingVisitor(
                BytecodeListingTextCollectingVisitor.Filter.EMPTY,
                allClasses,
                withSignatures = false,
                withAnnotations = false,
                sortDeclarations = true
            )

            node.accept(visitor)
            visitor.text
        }
    }

    object Directives : SimpleDirectivesContainer() {
        val CODE_FRAGMENT_CLASS_NAME by stringDirective(
            "Short name of a code fragment class",
            applicability = DirectiveApplicability.File
        )

        val CODE_FRAGMENT_METHOD_NAME by stringDirective(
            "Name of a code fragment facade method",
            applicability = DirectiveApplicability.File
        )

        val ATTACH_DUPLICATE_STDLIB by directive(
            "Attach the 'stdlib-jvm-minimal-for-test' library to simulate duplicate stdlib dependency"
        )

        val CHECK_CALLS_WITH_ANNOTATION by stringDirective(
            "Check whether all functions of calls and getters of properties with a given annotation are listed in *.check_calls.txt or not"
        )

        val CODE_COMPILATION_EXCEPTION by directive(
            "An exception caused by CodeGen API i.e., ${KaCodeCompilationException::class.jvmName} is expected"
        )
    }
}

private class CompilerFacilityEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (module.directives.contains(AbstractCompilerFacilityTest.Directives.ATTACH_DUPLICATE_STDLIB)) {
            configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(ForTestCompileRuntime.minimalRuntimeJarForTests()))
        }
    }
}

internal fun createCodeFragment(ktFile: KtFile, module: TestModule, testServices: TestServices): KtCodeFragment? {
    val ioFile = module.files.single { it.name == ktFile.name }.originalFile
    val ioFragmentFile = File(ioFile.parent, "${ioFile.nameWithoutExtension}.fragment.${ioFile.extension}")

    if (!ioFragmentFile.exists()) {
        return null
    }

    val contextElement = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtElement>(ktFile)

    val fragmentText = ioFragmentFile.readText()
    val isBlockFragment = fragmentText.any { it == '\n' }

    val project = ktFile.project
    val factory = KtPsiFactory(project, markGenerated = false)

    return when {
        isBlockFragment -> factory.createBlockCodeFragment(fragmentText, contextElement)
        else -> factory.createExpressionCodeFragment(fragmentText, contextElement)
    }
}

private class CollectingIrGenerationExtension(private val annotationToCheckCalls: String?) : IrGenerationExtension {
    var result: String = ""

    val functionsWithAnnotationToCheckCalls: MutableSet<String> = mutableSetOf()

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val dumpOptions = DumpIrTreeOptions(
            normalizeNames = true,
            stableOrder = true,
            printModuleName = false,
            printFilePath = false
        )

        result = moduleFragment.dump(dumpOptions)

        annotationToCheckCalls?.let { annotationFqName ->
            moduleFragment.accept(
                CheckCallsWithAnnotationVisitor(annotationFqName) { functionsWithAnnotationToCheckCalls.add(it.name.asString()) }, null
            )
        }
    }

    /**
     * This class recursively visits all calls of functions and getters, and if the function or the getter used for a call has
     * an annotation whose FqName is [annotationFqName], it runs [handleFunctionWithAnnotation] for the function or the getter.
     */
    private class CheckCallsWithAnnotationVisitor(
        private val annotationFqName: String,
        private val handleFunctionWithAnnotation: (declaration: IrDeclarationWithName) -> Unit,
    ) : IrElementVisitorVoid {
        val annotationClassId by lazy {
            val annotationFqNameUnsafe = FqNameUnsafe(annotationFqName)
            ClassId(FqName(annotationFqNameUnsafe.parent()), FqName(annotationFqNameUnsafe.shortName().asString()), false)
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall) {
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            val function = expression.symbol.owner
            if (function.containsAnnotationToCheckCalls()) {
                handleFunctionWithAnnotation(function)
            }
        }

        override fun visitFieldAccess(expression: IrFieldAccessExpression) {
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            val field = expression.symbol.owner
            if (field.containsAnnotationToCheckCalls()) {
                handleFunctionWithAnnotation(field)
            }
        }

        private fun IrAnnotationContainer.containsAnnotationToCheckCalls() =
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            annotations.any { it.symbol.owner.parentClassId == annotationClassId }
    }
}
