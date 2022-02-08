/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import junit.framework.AssertionFailedError
import junit.framework.TestCase
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.TestsCompilerError
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.SimpleGlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.frontend.java.di.createContainerForLazyResolveWithJava
import org.jetbrains.kotlin.frontend.java.di.initJvmBuiltInsForTopDownAnalysis
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.serialization.deserialization.MetadataPartProvider
import org.jetbrains.kotlin.storage.ExceptionTracker
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinBaseTest
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.DescriptorValidator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE_ALL
import org.jetbrains.kotlin.utils.keysToMap
import org.junit.Assert
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*
import java.util.function.Predicate
import java.util.regex.Pattern

@ObsoleteTestInfrastructure("org.jetbrains.kotlin.test.runners.AbstractDiagnosticTest")
abstract class AbstractDiagnosticsTest : BaseDiagnosticsTest() {
    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        try {
            analyzeAndCheckUnhandled(testDataFile, files)
        } catch (t: AssertionError) {
            throw t
        } catch (t: AssertionFailedError) {
            throw t
        } catch (t: Throwable) {
            throw TestsCompilerError(t)
        }
    }

    protected open fun shouldValidateFirTestData(testDataFile: File): Boolean {
        return false
    }

    private fun analyzeAndCheckUnhandled(testDataFile: File, files: List<TestFile>) {
        val groupedByModule = files.groupBy(TestFile::module)

        var lazyOperationsLog: LazyOperationsLog? = null

        val tracker = ExceptionTracker()
        val storageManager: StorageManager
        if (files.any(TestFile::checkLazyLog)) {
            lazyOperationsLog = LazyOperationsLog(HASH_SANITIZER)
            storageManager = LoggingStorageManager(
                LockBasedStorageManager.createWithExceptionHandling("AbstractDiagnosticTest", tracker),
                lazyOperationsLog.addRecordFunction
            )
        } else {
            storageManager = LockBasedStorageManager.createWithExceptionHandling("AbstractDiagnosticTest", tracker)
        }

        val context = SimpleGlobalContext(storageManager, tracker)

        val modules = createModules(groupedByModule, context.storageManager)
        val moduleBindings = HashMap<TestModule?, BindingContext>()

        val languageVersionSettingsByModule = HashMap<TestModule?, LanguageVersionSettings>()

        for ((testModule, testFilesInModule) in groupedByModule) {
            val ktFiles = getKtFiles(testFilesInModule, true)

            val oldModule = modules[testModule]!!

            val languageVersionSettings = loadLanguageVersionSettings(testFilesInModule)

            languageVersionSettingsByModule[testModule] = languageVersionSettings

            val moduleContext = context.withProject(project).withModule(oldModule)

            val separateModules = groupedByModule.size == 1 && groupedByModule.keys.single() == null
            val result = analyzeModuleContents(
                moduleContext, ktFiles, NoScopeRecordCliBindingTrace(),
                languageVersionSettings, separateModules, loadJvmTarget(testFilesInModule)
            )
            if (oldModule != result.moduleDescriptor) {
                // For common modules, we use DefaultAnalyzerFacade who creates ModuleDescriptor instances by itself
                // (its API does not support working with a module created beforehand).
                // So, we should replace the old (effectively discarded) module with the new one everywhere in dependencies.
                // TODO: dirty hack, refactor this test so that it doesn't create ModuleDescriptor instances
                modules[testModule] = result.moduleDescriptor as ModuleDescriptorImpl
                for (module in modules.values) {
                    @Suppress("DEPRECATION")
                    val it = (module.testOnly_AllDependentModules as MutableList).listIterator()
                    while (it.hasNext()) {
                        if (it.next() == oldModule) {
                            it.set(result.moduleDescriptor as ModuleDescriptorImpl)
                        }
                    }
                }
            }

            moduleBindings[testModule] = result.bindingContext
            checkAllResolvedCallsAreCompleted(ktFiles, result.bindingContext, languageVersionSettings)
        }

        // We want to always create a test data file (txt) if it was missing,
        // but don't want to skip the following checks in case this one fails
        var exceptionFromLazyResolveLogValidation: Throwable? = null
        if (lazyOperationsLog != null) {
            exceptionFromLazyResolveLogValidation = checkLazyResolveLog(lazyOperationsLog, testDataFile)
        } else {
            val lazyLogFile = getLazyLogFile(testDataFile)
            assertFalse("No lazy log expected, but found: ${lazyLogFile.absolutePath}", lazyLogFile.exists())
        }

        var exceptionFromDescriptorValidation: Throwable? = null
        try {
            val expectedFile = getExpectedDescriptorFile(testDataFile, files)
            validateAndCompareDescriptorWithFile(expectedFile, files, modules)
        } catch (e: Throwable) {
            exceptionFromDescriptorValidation = e
        }

        // main checks
        var ok = true

        val diagnosticsFullTextByteArrayStream = ByteArrayOutputStream()
        val diagnosticsFullTextPrintStream = PrintStream(diagnosticsFullTextByteArrayStream)
        var shouldCheckDiagnosticsFullText = false
        val diagnosticsFullTextCollector =
            GroupingMessageCollector(
                PrintingMessageCollector(diagnosticsFullTextPrintStream, MessageRenderer.SYSTEM_INDEPENDENT_RELATIVE_PATHS, true),
                false
            )

        val actualText = StringBuilder()
        for (testFile in files) {
            val module: KotlinBaseTest.TestModule? = testFile.module
            val isCommonModule = modules[module]!!.platform.isCommon()
            val implementingModules =
                if (!isCommonModule) emptyList()
                else modules.entries.filter { (testModule) -> module in testModule?.dependencies.orEmpty() }
            val implementingModulesBindings = implementingModules.mapNotNull { (testModule, moduleDescriptor) ->
                val platform = moduleDescriptor.platform
                if (platform != null && !platform.isCommon()) platform to moduleBindings[testModule]!!
                else null
            }
            val moduleDescriptor = modules[module]!!

            val moduleBindingContext = moduleBindings[module]!!
            ok = ok and testFile.getActualText(
                moduleBindingContext,
                implementingModulesBindings,
                actualText,
                shouldSkipJvmSignatureDiagnostics(groupedByModule) || isCommonModule,
                languageVersionSettingsByModule[module]!!,
                moduleDescriptor
            )

            if (testFile.renderDiagnosticsFullText) {
                shouldCheckDiagnosticsFullText = true
                AnalyzerWithCompilerReport.reportDiagnostics(
                    moduleBindingContext.diagnostics,
                    diagnosticsFullTextCollector,
                    renderInternalDiagnosticName = false
                )
            }
        }

        var exceptionFromDynamicCallDescriptorsValidation: Throwable? = null
        try {
            val expectedFile = File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".dynamic.txt")
            checkDynamicCallDescriptors(expectedFile, files)
        } catch (e: Throwable) {
            exceptionFromDynamicCallDescriptorsValidation = e
        }

        if (shouldCheckDiagnosticsFullText) {
            diagnosticsFullTextCollector.flush()
            diagnosticsFullTextPrintStream.flush()
            KotlinTestUtils.assertEqualsToFile(
                File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".diag.txt"),
                String(diagnosticsFullTextByteArrayStream.toByteArray())
            )
        }

        checkDiagnostics(actualText.cleanupInferenceDiagnostics(), testDataFile)

        assertTrue("Diagnostics mismatch. See the output above", ok)

        // now we throw a previously found error, if any
        exceptionFromDescriptorValidation?.let { throw it }
        exceptionFromLazyResolveLogValidation?.let { throw it }
        exceptionFromDynamicCallDescriptorsValidation?.let { throw it }

        performAdditionalChecksAfterDiagnostics(
            testDataFile, files, groupedByModule, modules, moduleBindings, languageVersionSettingsByModule
        )
    }

    protected open fun checkDiagnostics(actualText: String, testDataFile: File) {
        KotlinTestUtils.assertEqualsToFile(getExpectedDiagnosticsFile(testDataFile), actualText)
    }

    private fun StringBuilder.cleanupInferenceDiagnostics(): String = replace(Regex("NI;([\\S]*), OI;\\1([,!])")) { it.groupValues[1] + it.groupValues[2] }

    protected open fun getExpectedDiagnosticsFile(testDataFile: File): File {
        return testDataFile
    }

    protected open fun getExpectedDescriptorFile(testDataFile: File, files: List<TestFile>): File {
        val originalTestFileText = testDataFile.readText()

        val postfix = when {
            InTextDirectivesUtils.isDirectiveDefined(originalTestFileText, "// JAVAC_EXPECTED_FILE") &&
                    environment.configuration.getBoolean(JVMConfigurationKeys.USE_JAVAC) -> ".javac.txt"

            InTextDirectivesUtils.isDirectiveDefined(originalTestFileText, "// NI_EXPECTED_FILE") &&
                    files.any { it.newInferenceEnabled } && !USE_OLD_INFERENCE_DIAGNOSTICS_FOR_NI -> ".ni.txt"

            else -> ".txt"
        }

        return File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + postfix)
    }

    protected open fun performAdditionalChecksAfterDiagnostics(
        testDataFile: File,
        testFiles: List<TestFile>,
        moduleFiles: Map<TestModule?, List<TestFile>>,
        moduleDescriptors: Map<TestModule?, ModuleDescriptorImpl>,
        moduleBindings: Map<TestModule?, BindingContext>,
        languageVersionSettingsByModule: Map<TestModule?, LanguageVersionSettings>
    ) {
        // To be overridden by diagnostic-like tests.
    }

    protected open fun loadLanguageVersionSettings(module: List<TestFile>): LanguageVersionSettings {
        var result: LanguageVersionSettings? = null
        for (file in module) {
            val current = file.customLanguageVersionSettings
            if (current != null) {
                if (result != null && result != current) {
                    Assert.fail(
                        "More than one file in the module has $LANGUAGE_DIRECTIVE or $API_VERSION_DIRECTIVE directive specified. " +
                                "This is not supported. Please move all directives into one file"
                    )
                }
                result = current
            }
        }

        return result ?: defaultLanguageVersionSettings()
    }

    /**
     * Version settings used when no test data files have overriding version directives
     */
    protected open fun defaultLanguageVersionSettings(): LanguageVersionSettings {
        return CompilerTestLanguageVersionSettings(
            DEFAULT_DIAGNOSTIC_TESTS_FEATURES,
            LanguageVersionSettingsImpl.DEFAULT.apiVersion,
            LanguageVersionSettingsImpl.DEFAULT.languageVersion
        )
    }

    protected open fun loadJvmTarget(module: List<TestFile>): JvmTarget {
        var result: JvmTarget? = null
        for (file in module) {
            val current = file.jvmTarget
            if (current != null) {
                if (result != null && result != current) {
                    Assert.fail(
                        "More than one file in the module has $JVM_TARGET directive specified. " +
                                "This is not supported. Please move all directives into one file"
                    )
                }
                result = current
            }
        }

        return result ?: JvmTarget.DEFAULT
    }

    private fun checkDynamicCallDescriptors(expectedFile: File, testFiles: List<TestFile>) {
        val serializer = RecursiveDescriptorComparator(RECURSIVE_ALL)

        val actualText = StringBuilder()

        for (testFile in testFiles) {
            for (descriptor in testFile.dynamicCallDescriptors) {
                val actualSerialized = serializer.serializeRecursively(descriptor)
                actualText.append(actualSerialized)
            }
        }

        if (actualText.isNotEmpty() || expectedFile.exists()) {
            KotlinTestUtils.assertEqualsToFile(expectedFile, actualText.toString())
        }
    }

    protected open fun shouldSkipJvmSignatureDiagnostics(groupedByModule: Map<TestModule?, List<TestFile>>): Boolean =
        groupedByModule.size > 1

    private fun checkLazyResolveLog(lazyOperationsLog: LazyOperationsLog, testDataFile: File): Throwable? =
        try {
            val expectedFile = getLazyLogFile(testDataFile)
            KotlinTestUtils.assertEqualsToFile(expectedFile, lazyOperationsLog.getText(), HASH_SANITIZER)
            null
        } catch (e: Throwable) {
            e
        }

    private fun getLazyLogFile(testDataFile: File): File =
        File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".lazy.log")

    protected open fun analyzeModuleContents(
        moduleContext: ModuleContext,
        files: List<KtFile>,
        moduleTrace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
        separateModules: Boolean,
        jvmTarget: JvmTarget
    ): AnalysisResult {
        @Suppress("NAME_SHADOWING")
        var files = files

        // New JavaDescriptorResolver is created for each module, which is good because it emulates different Java libraries for each module,
        // albeit with same class names
        // See TopDownAnalyzerFacadeForJVM#analyzeFilesWithJavaIntegration

        // Temporary solution: only use separate module mode in single-module tests because analyzeFilesWithJavaIntegration
        // only supports creating two modules, whereas there can be more than two in multi-module diagnostic tests
        // TODO: always use separate module mode, once analyzeFilesWithJavaIntegration can create multiple modules
        if (separateModules) {
            return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                moduleContext.project,
                files,
                moduleTrace,
                environment.configuration.copy().apply {
                    this.languageVersionSettings = languageVersionSettings
                    this.put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
                },
                environment::createPackagePartProvider
            )
        }

        val moduleDescriptor = moduleContext.module as ModuleDescriptorImpl

        val platform = moduleDescriptor.platform
        if (platform.isCommon()) {
            return CommonResolverForModuleFactory.analyzeFiles(
                files, moduleDescriptor.name, true, languageVersionSettings,
                CommonPlatforms.defaultCommonPlatform, CompilerEnvironment,
                mapOf(
                    MODULE_FILES to files
                )
            ) { _ ->
                // TODO
                MetadataPartProvider.Empty
            }
        } else if (platform != null) {
            // TODO: analyze with the correct platform, not always JVM
            files += getCommonCodeFilesForPlatformSpecificModule(moduleDescriptor)
        }

        val moduleContentScope = GlobalSearchScope.allScope(moduleContext.project)
        val moduleClassResolver = SingleModuleClassResolver()

        val container = createContainerForLazyResolveWithJava(
            JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget), // TODO(dsavvinov): do not pass JvmTarget around
            moduleContext,
            moduleTrace,
            FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
            moduleContentScope,
            moduleClassResolver,
            CompilerEnvironment,
            LookupTracker.DO_NOTHING, ExpectActualTracker.DoNothing, InlineConstTracker.DoNothing,
            environment.createPackagePartProvider(moduleContentScope),
            languageVersionSettings,
            useBuiltInsProvider = true
        )

        container.initJvmBuiltInsForTopDownAnalysis()
        moduleClassResolver.resolver = container.get<JavaDescriptorResolver>()

        moduleDescriptor.initialize(
            CompositePackageFragmentProvider(
                listOf(
                    container.get<KotlinCodeAnalyzer>().packageFragmentProvider,
                    container.get<JavaDescriptorResolver>().packageFragmentProvider
                ),
                "CompositeProvider@AbstractDiagnosticsTest for $moduleDescriptor"
            )
        )

        container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)

        return AnalysisResult.success(moduleTrace.bindingContext, moduleDescriptor)
    }

    private fun getCommonCodeFilesForPlatformSpecificModule(moduleDescriptor: ModuleDescriptorImpl): List<KtFile> {
        // We assume that a platform-specific module _implements_ all declarations from common modules which are immediate dependencies.
        // So we collect all sources from such modules to analyze in the platform-specific module as well
        @Suppress("DEPRECATION")
        val dependencies = moduleDescriptor.testOnly_AllDependentModules

        // TODO: diagnostics on common code reported during the platform module analysis should be distinguished somehow
        // E.g. "<!JVM:ACTUAL_WITHOUT_EXPECT!>...<!>
        val result = ArrayList<KtFile>(0)
        for (dependency in dependencies) {
            if (dependency.platform.isCommon()) {
                val files = dependency.getCapability(MODULE_FILES)
                        ?: error("MODULE_FILES should have been set for the common module: $dependency")
                result.addAll(files)
            }
        }

        return result
    }

    private fun validateAndCompareDescriptorWithFile(
        expectedFile: File,
        testFiles: List<TestFile>,
        modules: Map<TestModule?, ModuleDescriptorImpl>
    ) {
        if (skipDescriptorsValidation()) return
        if (testFiles.any { file -> InTextDirectivesUtils.isDirectiveDefined(file.expectedText, "// SKIP_TXT") }) {
            assertFalse(".txt file should not exist if SKIP_TXT directive is used: $expectedFile", expectedFile.exists())
            return
        }

        val comparator = RecursiveDescriptorComparator(
            createdAffectedPackagesConfiguration(
                testFiles,
                modules.values
            )
        )

        val isMultiModuleTest = modules.size != 1

        val packages =
            (testFiles.flatMap {
                InTextDirectivesUtils.findListWithPrefixes(it.expectedText, "// RENDER_PACKAGE:").map {
                    FqName(it.trim())
                }
            } + FqName.ROOT).toSet()

        val textByPackage = packages.keysToMap { StringBuilder() }

        val sortedModules = modules.keys.sortedWith(Comparator { x, y ->
            when {
                x == null && y == null -> 0
                x == null && y != null -> -1
                x != null && y == null -> 1
                x != null && y != null -> x.compareTo(y)
                else -> error("Unreachable")
            }
        })

        for ((packageName, packageText) in textByPackage.entries) {
            val module = sortedModules.iterator()
            while (module.hasNext()) {
                val moduleDescriptor = modules[module.next()]!!

                val aPackage = moduleDescriptor.getPackage(packageName)
                assertFalse(aPackage.isEmpty())

                if (isMultiModuleTest) {
                    packageText.append(String.format("// -- Module: %s --\n", moduleDescriptor.name))
                }

                val actualSerialized = comparator.serializeRecursively(aPackage)
                packageText.append(actualSerialized)

                if (isMultiModuleTest && module.hasNext()) {
                    packageText.append("\n\n")
                }
            }
        }

        val allPackagesText = textByPackage.values.joinToString("\n")

        val lineCount = StringUtil.getLineBreakCount(allPackagesText)
        assert(lineCount < 1000) {
            "Rendered descriptors of this test take up $lineCount lines. " +
                    "Please ensure you don't render JRE contents to the .txt file. " +
                    "Such tests are hard to maintain, take long time to execute and are subject to sudden unreviewed changes anyway."
        }

        KotlinTestUtils.assertEqualsToFile(expectedFile, allPackagesText)
    }


    protected open fun skipDescriptorsValidation(): Boolean = false

    private fun getJavaFilePackage(testFile: TestFile): Name {
        val pattern = Pattern.compile("^\\s*package [.\\w\\d]*", Pattern.MULTILINE)
        val matcher = pattern.matcher(testFile.expectedText)

        if (matcher.find()) {
            return testFile.expectedText
                .substring(matcher.start(), matcher.end())
                .split(" ")
                .last()
                .filter { !it.isWhitespace() }
                .let { Name.identifier(it.split(".").first()) }
        }

        return SpecialNames.ROOT_PACKAGE
    }

    private fun createdAffectedPackagesConfiguration(
        testFiles: List<TestFile>,
        modules: Collection<ModuleDescriptor>
    ): RecursiveDescriptorComparator.Configuration {
        val packagesNames = (
                testFiles.filter { it.ktFile == null }
                    .map { getJavaFilePackage(it) } +
                        getTopLevelPackagesFromFileList(getKtFiles(testFiles, false))
                ).toSet()

        val checkTypeEnabled = testFiles.any { it.declareCheckType }
        val stepIntoFilter = Predicate<DeclarationDescriptor> { descriptor ->
            val module = DescriptorUtils.getContainingModuleOrNull(descriptor)
            if (module !in modules) return@Predicate false

            if (descriptor is PackageViewDescriptor) {
                val fqName = descriptor.fqName
                return@Predicate fqName.isRoot || fqName.pathSegments().first() in packagesNames
            }

            if (checkTypeEnabled && descriptor.name in NAMES_OF_CHECK_TYPE_HELPER) return@Predicate false

            true
        }

        return RECURSIVE.filterRecursion(stepIntoFilter)
            .withValidationStrategy(DescriptorValidator.ValidationVisitor.errorTypesAllowed())
            .checkFunctionContracts(true)
    }

    private fun getTopLevelPackagesFromFileList(files: List<KtFile>): Set<Name> =
        files.mapTo(LinkedHashSet<Name>()) { file ->
            file.packageFqName.pathSegments().firstOrNull() ?: SpecialNames.ROOT_PACKAGE
        }

    private fun createModules(
        groupedByModule: Map<TestModule?, List<TestFile>>,
        storageManager: StorageManager
    ): MutableMap<TestModule?, ModuleDescriptorImpl> {
        val modules = HashMap<TestModule?, ModuleDescriptorImpl>()

        for (testModule in groupedByModule.keys) {
            val module = if (testModule == null)
                createSealedModule(storageManager)
            else
                createModule(testModule.name, storageManager)

            modules.put(testModule, module)
        }

        for (testModule in groupedByModule.keys) {
            if (testModule == null) continue

            val module = modules[testModule]!!
            val dependencies = ArrayList<ModuleDescriptorImpl>()
            dependencies.add(module)
            for (dependency in testModule.dependencies) {
                dependencies.add(modules[dependency as TestModule?]!!)
            }

            dependencies.add(module.builtIns.builtInsModule)
            dependencies.addAll(getAdditionalDependencies(module))
            module.setDependencies(dependencies)
        }

        return modules
    }

    protected open fun getAdditionalDependencies(module: ModuleDescriptorImpl): List<ModuleDescriptorImpl> =
        emptyList()

    protected open fun createModule(moduleName: String, storageManager: StorageManager): ModuleDescriptorImpl {
        val platform = parseModulePlatformByName(moduleName)
        val builtIns = JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FROM_CLASS_LOADER)
        return ModuleDescriptorImpl(Name.special("<$moduleName>"), storageManager, builtIns, platform)
    }

    protected open fun createSealedModule(storageManager: StorageManager): ModuleDescriptorImpl =
        createModule("test-module-jvm", storageManager).apply {
            setDependencies(this, builtIns.builtInsModule)
        }

    private fun checkAllResolvedCallsAreCompleted(
        ktFiles: List<KtFile>,
        bindingContext: BindingContext,
        configuredLanguageVersionSettings: LanguageVersionSettings
    ) {
        if (ktFiles.any { file -> AnalyzingUtils.getSyntaxErrorRanges(file).isNotEmpty() }) return

        val resolvedCallsEntries = bindingContext.getSliceContents(BindingContext.RESOLVED_CALL)
        val unresolvedCallsOnElements = ArrayList<PsiElement>()

        for ((call, resolvedCall) in resolvedCallsEntries) {
            val element = call.callElement

            if (!configuredLanguageVersionSettings.supportsFeature(LanguageFeature.NewInference)) {
                if (!(resolvedCall as MutableResolvedCall<*>).isCompleted) {
                    unresolvedCallsOnElements.add(element)
                }
            }
        }

        if (unresolvedCallsOnElements.isNotEmpty()) {
            TestCase.fail(
                "There are uncompleted resolved calls for the following elements:\n" +
                        unresolvedCallsOnElements.joinToString(separator = "\n") { element ->
                            val lineAndColumn = DiagnosticUtils.getLineAndColumnInPsiFile(element.containingFile, element.textRange)
                            "'${element.text}'$lineAndColumn"
                        }
            )
        }

        checkResolvedCallsInDiagnostics(bindingContext, configuredLanguageVersionSettings)
    }

    private fun checkResolvedCallsInDiagnostics(
        bindingContext: BindingContext,
        configuredLanguageVersionSettings: LanguageVersionSettings
    ) {
        val diagnosticsStoringResolvedCalls1 = setOf(
            OVERLOAD_RESOLUTION_AMBIGUITY, NONE_APPLICABLE, CANNOT_COMPLETE_RESOLVE, UNRESOLVED_REFERENCE_WRONG_RECEIVER,
            ASSIGN_OPERATOR_AMBIGUITY, ITERATOR_AMBIGUITY
        )
        val diagnosticsStoringResolvedCalls2 = setOf(
            COMPONENT_FUNCTION_AMBIGUITY, DELEGATE_SPECIAL_FUNCTION_AMBIGUITY, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE
        )

        for (diagnostic in bindingContext.diagnostics) {
            when (diagnostic.factory) {
                in diagnosticsStoringResolvedCalls1 -> assertResolvedCallsAreCompleted(
                    diagnostic, DiagnosticFactory.cast(diagnostic, diagnosticsStoringResolvedCalls1).a, configuredLanguageVersionSettings
                )
                in diagnosticsStoringResolvedCalls2 -> assertResolvedCallsAreCompleted(
                    diagnostic, DiagnosticFactory.cast(diagnostic, diagnosticsStoringResolvedCalls2).b, configuredLanguageVersionSettings
                )
            }
        }
    }

    private fun assertResolvedCallsAreCompleted(
        diagnostic: Diagnostic,
        resolvedCalls: Collection<ResolvedCall<*>>,
        configuredLanguageVersionSettings: LanguageVersionSettings
    ) {
        val element = diagnostic.psiElement
        val lineAndColumn = DiagnosticUtils.getLineAndColumnInPsiFile(element.containingFile, element.textRange)
        if (configuredLanguageVersionSettings.supportsFeature(LanguageFeature.NewInference)) return

        assertTrue("Resolved calls stored in ${diagnostic.factory.name}\nfor '${element.text}'$lineAndColumn are not completed",
                   resolvedCalls.all { (it as MutableResolvedCall<*>).isCompleted })
    }

    companion object {
        private val HASH_SANITIZER = fun(s: String): String = s.replace("@(\\d)+".toRegex(), "")

        private val MODULE_FILES = ModuleCapability<List<KtFile>>("")

        private val NAMES_OF_CHECK_TYPE_HELPER = listOf("checkSubtype", "CheckTypeInv", "_", "checkType").map { Name.identifier(it) }
    }
}
