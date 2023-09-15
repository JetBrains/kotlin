/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import junit.framework.TestCase
import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.backend.common.ir.isExpect
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataIncrementalSerializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.makeSerializedKlibMetadata
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.AbstractIrGeneratorTestCase
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.KotlinFileSerializedData
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrModuleSerializer
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.buildKotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.utils.rethrow
import java.io.File
import java.util.*

/**
 * Compares compiled and deserialized IR
 */
abstract class AbstractKlibIrTextTestCase : CodegenTestCase() {

    companion object {
        val SKIP_KLIB_TEST = Regex("""// SKIP_KLIB_TEST""")
        const val MODULE_NAME = "testModule"
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {

        if (SKIP_KLIB_TEST.containsMatchIn(wholeFile.readText())) return

        setupEnvironment(files)

        loadMultiFiles(files)
        doTest(wholeFile)
    }

    private fun setupEnvironment(files: List<TestFile>) {
        val configuration = createConfiguration(
            ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, backend,
            listOf<File>(KtTestUtil.getAnnotationsJar()),
            listOfNotNull(writeJavaFiles(files)),
            files
        )
        configuration.put(CommonConfigurationKeys.MODULE_NAME, MODULE_NAME)
        myEnvironment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
    }

    fun doTest(wholeFile: File) {
        val ignoreErrors = AbstractIrGeneratorTestCase.shouldIgnoreErrors(wholeFile)

        val stdlib = loadKlibFromPath(listOf(runtimeKlibPath)).single()
        val (irModule, bindingContext) = generateIrModule(stdlib, ignoreErrors)
        irModule.cleanUpFromExpectDeclarations()

        val expected = irModule.dump(DumpIrTreeOptions(stableOrder = true, verboseErrorTypes = false))

        val klibPath = serializeModule(irModule, bindingContext, stdlib, ignoreErrors)
        val libs = loadKlibFromPath(listOf(runtimeKlibPath, klibPath))
        val (stdlib2, klib) = libs
        val deserializedIrModule = deserializeModule(stdlib2, klib)

        val actual = deserializedIrModule.dump(DumpIrTreeOptions(stableOrder = true, verboseErrorTypes = false))

        try {
            TestCase.assertEquals(wholeFile.name, expected, actual)
        } catch (e: Throwable) {
            throw rethrow(e)
        }
    }

    private fun klibMetadataIncrementalSerializer(configuration: CompilerConfiguration, project: Project, allowErrors: Boolean) =
        KlibMetadataIncrementalSerializer(
            languageVersionSettings = configuration.languageVersionSettings,
            metadataVersion = KlibMetadataVersion.INSTANCE,
            project = project,
            exportKDoc = false,
            allowErrorTypes = allowErrors
        )

    private fun getDescriptorForElement(
        context: BindingContext,
        element: PsiElement
    ): DeclarationDescriptor = BindingContextUtils.getNotNull(context, BindingContext.DECLARATION_TO_DESCRIPTOR, element)

    private fun KlibMetadataIncrementalSerializer.serializeScope(
        ktFile: KtFile,
        bindingContext: BindingContext,
        moduleDescriptor: ModuleDescriptor
    ): ProtoBuf.PackageFragment {
        val memberScope = ktFile.declarations.map { getDescriptorForElement(bindingContext, it) }
        return serializePackageFragment(moduleDescriptor, memberScope, ktFile.packageFqName)
    }

    protected fun serializeModule(irModuleFragment: IrModuleFragment, bindingContext: BindingContext, stdlib: KotlinLibrary, containsErrorCode: Boolean): String {
        val ktFiles = myFiles.psiFiles
        val serializedIr = JsIrModuleSerializer(
                IrMessageLogger.None,
                irModuleFragment.irBuiltins,
                CompatibilityMode.CURRENT,
                false,
                emptyList(),
                myEnvironment.configuration.languageVersionSettings,
            ).serializedIrModule(irModuleFragment)

        val moduleDescriptor = irModuleFragment.descriptor
        val metadataSerializer = klibMetadataIncrementalSerializer(myEnvironment.configuration, myEnvironment.project, containsErrorCode)


        val compiledKotlinFiles = mutableListOf<KotlinFileSerializedData>()
        for ((ktFile, binaryFile) in ktFiles.zip(serializedIr.files)) {
            assert(ktFile.virtualFilePath == binaryFile.path) {
                """The Kt and Ir files are put in different order
                Kt: ${ktFile.virtualFilePath}
                Ir: ${binaryFile.path}
            """.trimMargin()
            }
            val packageFragment = metadataSerializer.serializeScope(ktFile, bindingContext, moduleDescriptor)
            val compiledKotlinFile = KotlinFileSerializedData(packageFragment.toByteArray(), binaryFile)

            compiledKotlinFiles += compiledKotlinFile
        }

        val header = metadataSerializer.serializeHeader(
            moduleDescriptor,
            compiledKotlinFiles.map { it.irData.fqName }.distinct().sorted(),
            emptyList()
        ).toByteArray()


        val serializedMetadata = makeSerializedKlibMetadata(
            compiledKotlinFiles.groupBy { it.irData.fqName }
                .map { (fqn, data) -> fqn to data.sortedBy { it.irData.path }.map { it.metadata } }.toMap(),
            header
        )

        val fullSerializedIr = SerializedIrModule(compiledKotlinFiles.map { it.irData })

        val properties = Properties().also { p ->
            if (containsErrorCode) {
                p.setProperty(KLIB_PROPERTY_CONTAINS_ERROR_CODE, "true")
            }
        }

        val versions = KotlinLibraryVersioning(
            abiVersion = KotlinAbiVersion.CURRENT,
            libraryVersion = null,
            compilerVersion = KotlinCompilerVersion.VERSION,
            metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
        )

        val klibDir = org.jetbrains.kotlin.konan.file.createTempDir("testKlib")

        buildKotlinLibrary(
            linkDependencies = listOf(stdlib),
            ir = fullSerializedIr,
            metadata = serializedMetadata,
            dataFlowGraph = null,
            manifestProperties = properties,
            moduleName = MODULE_NAME,
            nopack = true,
            perFile = false,
            output = klibDir.canonicalPath,
            versions = versions,
            builtInsPlatform = BuiltInsPlatform.JS
        )

        return klibDir.canonicalPath
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    protected fun deserializeModule(stdlib: KotlinLibrary, klib: KotlinLibrary): IrModuleFragment {
        val signaturer = IdSignatureDescriptor(JsManglerDesc)

        val stdlibDescriptor = getModuleDescriptor(stdlib)
        val testDescriptor = getModuleDescriptor(klib, stdlibDescriptor)

        val symbolTable = SymbolTable(signaturer, IrFactoryImpl)
        val typeTranslator =
            TypeTranslatorImpl(symbolTable, myEnvironment.configuration.languageVersionSettings, testDescriptor)
        val irBuiltIns = IrBuiltInsOverDescriptors(testDescriptor.builtIns, typeTranslator, symbolTable)
        val irLinker = JsIrLinker(null, IrMessageLogger.None, irBuiltIns, symbolTable, PartialLinkageSupportForLinker.DISABLED, null)
        irLinker.deserializeIrModuleHeader(stdlibDescriptor, stdlib)
        val testModule = irLinker.deserializeIrModuleHeader(testDescriptor, klib, { DeserializationStrategy.ALL })
        irLinker.init(null, emptyList())
        ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
        irLinker.postProcess(inOrAfterLinkageStep = true)
        irLinker.clear()
        return testModule
    }

    private fun loadKlibFromPath(paths: List<String>): List<KotlinLibrary> {
        val result = CommonKLibResolver.resolve(paths, DummyLogger)
        return result.getFullList(TopologicalLibraryOrder)
    }

    private val runtimeKlibPath = "libraries/stdlib/build/classes/kotlin/js/main"

    /**
     * In multiplatform projects there may be `expect` declarations. Such declarations do not survive during KLIB serialization.
     * So, it's necessary to explicitly filter them out from the [IrModuleFragment] in order for tests that compare dumped IR
     * before and after serialization to pass successfully.
     */
    private fun IrModuleFragment.cleanUpFromExpectDeclarations() {
        val languageVersionSettings = myEnvironment.configuration.languageVersionSettings
        val multiPlatformProjects = languageVersionSettings.getFeatureSupport(LanguageFeature.MultiPlatformProjects)
        if (multiPlatformProjects != LanguageFeature.State.ENABLED)
            return

        acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

            override fun visitPackageFragment(declaration: IrPackageFragment) = visitDeclarationContainer(declaration)
            override fun visitClass(declaration: IrClass) = visitDeclarationContainer(declaration)

            private fun visitDeclarationContainer(container: IrDeclarationContainer) {
                container.declarations.removeIf(IrDeclaration::isExpect)
                visitElement(container)
            }
        })
    }

    fun getModuleDescriptor(current: KotlinLibrary, builtins: ModuleDescriptorImpl? = null): ModuleDescriptorImpl {
        val lookupTracker = LookupTracker.DO_NOTHING
        val md = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            current,
            myEnvironment.configuration.languageVersionSettings,
            LockBasedStorageManager("ModulesStructure"),
            builtins?.builtIns,
            packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
            lookupTracker = lookupTracker
        )
        md.setDependencies(listOfNotNull(builtins, md))
        return md
    }

    private fun generateIrModule(
        stdlib: KotlinLibrary,
        ignoreErrors: Boolean,
    ): Pair<IrModuleFragment, BindingContext> {
        val stdlibDescriptor = getModuleDescriptor(stdlib)

        val ktFiles = myFiles.psiFiles

        val messageLogger = IrMessageLogger.None
        val psi2Ir = Psi2IrTranslator(
            myEnvironment.configuration.languageVersionSettings,
            Psi2IrConfiguration(ignoreErrors),
            myEnvironment.configuration::checkNoUnboundSymbols
        )
        val analysisResult = TopDownAnalyzerFacadeForJS.analyzeFiles(
            ktFiles, myEnvironment.project, myEnvironment.configuration,
            moduleDescriptors = listOf(stdlibDescriptor),
            friendModuleDescriptors = emptyList(),
            CompilerEnvironment,
            customBuiltInsModule = stdlibDescriptor
        )

        val (bindingContext, moduleDescriptor) = analysisResult
        if (!psi2Ir.configuration.ignoreErrors) {
            analysisResult.throwIfError()
            AnalyzingUtils.throwExceptionOnErrors(bindingContext)
        }

        val symbolTable = SymbolTable(IdSignatureDescriptor(JsManglerDesc), IrFactoryImpl, NameProvider.DEFAULT)
        val context = psi2Ir.createGeneratorContext(moduleDescriptor, bindingContext, symbolTable)
        val irBuiltIns = context.irBuiltIns
        val irLinker = JsIrLinker(moduleDescriptor, messageLogger, irBuiltIns, symbolTable, PartialLinkageSupportForLinker.DISABLED, null)
        irLinker.deserializeIrModuleHeader(stdlibDescriptor, stdlib)

        return psi2Ir.generateModuleFragment(context, ktFiles, listOf(irLinker), emptyList()) to bindingContext
    }
}

abstract class AbstractKlibJsIrTextTestCase : AbstractKlibIrTextTestCase()
