/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.checkers

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.common.DefaultAnalyzerFacade
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.SimpleGlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.frontend.java.di.createContainerForTopDownAnalyzerForJvm
import org.jetbrains.kotlin.frontend.java.di.initJvmBuiltInsForTopDownAnalysis
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.USE_NEW_INFERENCE
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.storage.ExceptionTracker
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.DescriptorValidator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE_ALL
import org.jetbrains.kotlin.utils.keysToMap
import org.junit.Assert
import java.io.File
import java.util.*
import java.util.function.Predicate
import java.util.regex.Pattern

abstract class AbstractDiagnosticsTest : BaseDiagnosticsTest() {
    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        val groupedByModule = files.groupBy(TestFile::module)

        var lazyOperationsLog: LazyOperationsLog? = null

        val tracker = ExceptionTracker()
        val storageManager: StorageManager
        if (files.any(TestFile::checkLazyLog)) {
            lazyOperationsLog = LazyOperationsLog(HASH_SANITIZER)
            storageManager = LoggingStorageManager(
                    LockBasedStorageManager.createWithExceptionHandling(tracker),
                    lazyOperationsLog.addRecordFunction
            )
        }
        else {
            storageManager = LockBasedStorageManager.createWithExceptionHandling(tracker)
        }

        val context = SimpleGlobalContext(storageManager, tracker)

        val modules = createModules(groupedByModule, context.storageManager)
        val moduleBindings = HashMap<TestModule?, BindingContext>()

        for ((testModule, testFilesInModule) in groupedByModule) {
            val ktFiles = getKtFiles(testFilesInModule, true)

            val oldModule = modules[testModule]!!

            val languageVersionSettings = loadLanguageVersionSettings(testFilesInModule)
            val moduleContext = context.withProject(project).withModule(oldModule)

            val separateModules = groupedByModule.size == 1 && groupedByModule.keys.single() == null
            val result = analyzeModuleContents(
                    moduleContext, ktFiles, CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(),
                    languageVersionSettings, separateModules
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
            checkAllResolvedCallsAreCompleted(ktFiles, result.bindingContext)
        }

        // We want to always create a test data file (txt) if it was missing,
        // but don't want to skip the following checks in case this one fails
        var exceptionFromLazyResolveLogValidation: Throwable? = null
        if (lazyOperationsLog != null) {
            exceptionFromLazyResolveLogValidation = checkLazyResolveLog(lazyOperationsLog, testDataFile)
        }
        else {
            val lazyLogFile = getLazyLogFile(testDataFile)
            assertFalse("No lazy log expected, but found: ${lazyLogFile.absolutePath}", lazyLogFile.exists())
        }

        var exceptionFromDescriptorValidation: Throwable? = null
        try {
            val expectedFile = if (InTextDirectivesUtils.isDirectiveDefined(testDataFile.readText(), "// JAVAC_EXPECTED_FILE")
                                   && environment.configuration.getBoolean(JVMConfigurationKeys.USE_JAVAC)) {
                File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".javac.txt")
            } else {
                File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".txt")
            }
            validateAndCompareDescriptorWithFile(expectedFile, files, modules)
        }
        catch (e: Throwable) {
            exceptionFromDescriptorValidation = e
        }

        // main checks
        var ok = true

        val actualText = StringBuilder()
        for (testFile in files) {
            val module = testFile.module
            val isCommonModule = modules[module]!!.getMultiTargetPlatform() == MultiTargetPlatform.Common
            val implementingModules =
                    if (!isCommonModule) emptyList()
                    else modules.entries.filter { (testModule) -> module in testModule?.getDependencies().orEmpty() }
            val implementingModulesBindings = implementingModules.mapNotNull {
                (testModule, moduleDescriptor) ->
                val platform = moduleDescriptor.getCapability(MultiTargetPlatform.CAPABILITY)
                if (platform is MultiTargetPlatform.Specific) platform to moduleBindings[testModule]!!
                else null
            }
            ok = ok and testFile.getActualText(
                    moduleBindings[module]!!, implementingModulesBindings, actualText,
                    shouldSkipJvmSignatureDiagnostics(groupedByModule) || isCommonModule
            )
        }

        var exceptionFromDynamicCallDescriptorsValidation: Throwable? = null
        try {
            val expectedFile = File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".dynamic.txt")
            checkDynamicCallDescriptors(expectedFile, files)
        }
        catch (e: Throwable) {
            exceptionFromDynamicCallDescriptorsValidation = e
        }

        KotlinTestUtils.assertEqualsToFile(testDataFile, actualText.toString())

        assertTrue("Diagnostics mismatch. See the output above", ok)

        // now we throw a previously found error, if any
        exceptionFromDescriptorValidation?.let { throw it }
        exceptionFromLazyResolveLogValidation?.let { throw it }
        exceptionFromDynamicCallDescriptorsValidation?.let { throw it }

        performAdditionalChecksAfterDiagnostics(testDataFile, files, groupedByModule, modules, moduleBindings)
    }

    protected open fun performAdditionalChecksAfterDiagnostics(
            testDataFile: File,
            testFiles: List<TestFile>,
            moduleFiles: Map<TestModule?, List<TestFile>>,
            moduleDescriptors: Map<TestModule?, ModuleDescriptorImpl>,
            moduleBindings: Map<TestModule?, BindingContext>
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

        return result ?: BaseDiagnosticsTest.DiagnosticTestLanguageVersionSettings(
                BaseDiagnosticsTest.DEFAULT_DIAGNOSTIC_TESTS_FEATURES,
                LanguageVersionSettingsImpl.DEFAULT.apiVersion,
                LanguageVersionSettingsImpl.DEFAULT.languageVersion
        )
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
            }
            catch (e: Throwable) {
                e
            }

    private fun getLazyLogFile(testDataFile: File): File =
            File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".lazy.log")

    protected open fun analyzeModuleContents(
            moduleContext: ModuleContext,
            files: List<KtFile>,
            moduleTrace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
            separateModules: Boolean
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
                    environment.configuration.copy().apply { this.languageVersionSettings = languageVersionSettings },
                    environment::createPackagePartProvider
            )
        }

        val moduleDescriptor = moduleContext.module as ModuleDescriptorImpl

        val platform = moduleDescriptor.getMultiTargetPlatform()
        if (platform == MultiTargetPlatform.Common) {
            return DefaultAnalyzerFacade.analyzeFiles(
                    files, moduleDescriptor.name, true, languageVersionSettings,
                    mapOf(
                            MultiTargetPlatform.CAPABILITY to MultiTargetPlatform.Common,
                            MODULE_FILES to files
                    )
            ) { _, _ ->
                // TODO
                PackagePartProvider.Empty
            }
        }
        else if (platform != null) {
            // TODO: analyze with the correct platform, not always JVM
            files += getCommonCodeFilesForPlatformSpecificModule(moduleDescriptor)
        }

        val moduleContentScope = GlobalSearchScope.allScope(moduleContext.project)
        val moduleClassResolver = SingleModuleClassResolver()

        val container = createContainerForTopDownAnalyzerForJvm(
                moduleContext,
                moduleTrace,
                FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
                moduleContentScope,
                LookupTracker.DO_NOTHING,
                environment.createPackagePartProvider(moduleContentScope),
                moduleClassResolver,
                JvmTarget.JVM_1_6,
                languageVersionSettings
        )

        container.initJvmBuiltInsForTopDownAnalysis()
        moduleClassResolver.resolver = container.get<JavaDescriptorResolver>()

        moduleDescriptor.initialize(CompositePackageFragmentProvider(listOf(
                container.get<KotlinCodeAnalyzer>().packageFragmentProvider,
                container.get<JavaDescriptorResolver>().packageFragmentProvider
        )))

        container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)

        return AnalysisResult.success(moduleTrace.bindingContext, moduleDescriptor)
    }

    private fun getCommonCodeFilesForPlatformSpecificModule(moduleDescriptor: ModuleDescriptorImpl): List<KtFile> {
        // We assume that a platform-specific module _implements_ all declarations from common modules which are immediate dependencies.
        // So we collect all sources from such modules to analyze in the platform-specific module as well
        @Suppress("DEPRECATION")
        val dependencies = moduleDescriptor.testOnly_AllDependentModules

        // TODO: diagnostics on common code reported during the platform module analysis should be distinguished somehow
        // E.g. "<!JVM:IMPLEMENTATION_WITHOUT_HEADER!>...<!>
        val result = ArrayList<KtFile>(0)
        for (dependency in dependencies) {
            if (dependency.getCapability(MultiTargetPlatform.CAPABILITY) == MultiTargetPlatform.Common) {
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

        val comparator = RecursiveDescriptorComparator(createdAffectedPackagesConfiguration(testFiles, modules.values))

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

        val stepIntoFilter = Predicate<DeclarationDescriptor> { descriptor ->
            val module = DescriptorUtils.getContainingModuleOrNull(descriptor)
            if (module !in modules) return@Predicate false

            if (descriptor is PackageViewDescriptor) {
                val fqName = descriptor.fqName
                return@Predicate fqName.isRoot || fqName.pathSegments().first() in packagesNames
            }

            true
        }

        return RECURSIVE.filterRecursion(stepIntoFilter).withValidationStrategy(DescriptorValidator.ValidationVisitor.errorTypesAllowed())
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
            for (dependency in testModule.getDependencies()) {
                dependencies.add(modules[dependency]!!)
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
        val nameSuffix = moduleName.substringAfterLast("-", "")
        val platform =
                if (nameSuffix.isEmpty()) null
                else if (nameSuffix == "common") MultiTargetPlatform.Common else MultiTargetPlatform.Specific(nameSuffix.toUpperCase())
        return ModuleDescriptorImpl(Name.special("<$moduleName>"), storageManager, JvmBuiltIns(storageManager), platform)
    }

    protected open fun createSealedModule(storageManager: StorageManager): ModuleDescriptorImpl =
            createModule("test-module", storageManager).apply {
                setDependencies(this, builtIns.builtInsModule)
            }

    private fun checkAllResolvedCallsAreCompleted(ktFiles: List<KtFile>, bindingContext: BindingContext) {
        if (ktFiles.any { file -> AnalyzingUtils.getSyntaxErrorRanges(file).isNotEmpty() }) return

        val resolvedCallsEntries = bindingContext.getSliceContents(BindingContext.RESOLVED_CALL)
        for ((call, resolvedCall) in resolvedCallsEntries) {
            val element = call.callElement

            val lineAndColumn = DiagnosticUtils.getLineAndColumnInPsiFile(element.containingFile, element.textRange)

            if (!USE_NEW_INFERENCE) {
                assertTrue("Resolved call for '${element.text}'$lineAndColumn is not completed",
                           (resolvedCall as MutableResolvedCall<*>).isCompleted)
            }
        }

        checkResolvedCallsInDiagnostics(bindingContext)
    }

    private fun checkResolvedCallsInDiagnostics(bindingContext: BindingContext) {
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
                        diagnostic, DiagnosticFactory.cast(diagnostic, diagnosticsStoringResolvedCalls1).a
                )
                in diagnosticsStoringResolvedCalls2 -> assertResolvedCallsAreCompleted(
                        diagnostic, DiagnosticFactory.cast(diagnostic, diagnosticsStoringResolvedCalls2).b
                )
            }
        }
    }

    private fun assertResolvedCallsAreCompleted(diagnostic: Diagnostic, resolvedCalls: Collection<ResolvedCall<*>>) {
        val element = diagnostic.psiElement
        val lineAndColumn = DiagnosticUtils.getLineAndColumnInPsiFile(element.containingFile, element.textRange)
        if (USE_NEW_INFERENCE) return

        assertTrue("Resolved calls stored in ${diagnostic.factory.name}\nfor '${element.text}'$lineAndColumn are not completed",
                   resolvedCalls.all { (it as MutableResolvedCall<*>).isCompleted })
    }

    companion object {
        private val HASH_SANITIZER = fun(s: String): String = s.replace("@(\\d)+".toRegex(), "")

        private val MODULE_FILES = ModuleDescriptor.Capability<List<KtFile>>("")
    }
}
