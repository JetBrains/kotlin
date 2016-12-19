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

package org.jetbrains.kotlin.checkers

import com.google.common.base.Predicate
import com.google.common.collect.Sets
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import junit.framework.TestCase
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.common.DefaultAnalyzerFacade
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.context.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.frontend.java.di.createContainerForTopDownAnalyzerForJvm
import org.jetbrains.kotlin.frontend.java.di.initJvmBuiltInsForTopDownAnalysis
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.storage.ExceptionTracker
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.DescriptorValidator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.*
import org.jetbrains.kotlin.utils.rethrow
import org.junit.Assert
import java.io.File
import java.util.*

abstract class AbstractDiagnosticsTest : BaseDiagnosticsTest() {
    override fun analyzeAndCheck(testDataFile: File, testFiles: List<BaseDiagnosticsTest.TestFile>) {
        val groupedByModule = testFiles.groupByTo<TestFile, TestModule, LinkedHashMap<TestModule, List<TestFile>>>(
                LinkedHashMap<TestModule, List<TestFile>>()
        ) { file -> file.module }

        val checkLazyResolveLog = testFiles.any { file -> file.checkLazyLog }

        var lazyOperationsLog: LazyOperationsLog? = null
        val context: GlobalContext

        val tracker = ExceptionTracker()
        if (checkLazyResolveLog) {
            lazyOperationsLog = LazyOperationsLog(HASH_SANITIZER)
            context = SimpleGlobalContext(
                    LoggingStorageManager(
                            LockBasedStorageManager.createWithExceptionHandling(tracker),
                            lazyOperationsLog.addRecordFunction
                    ),
                    tracker
            )
        }
        else {
            context = SimpleGlobalContext(LockBasedStorageManager.createWithExceptionHandling(tracker), tracker)
        }

        val modules = createModules(groupedByModule, context.storageManager)
        val moduleBindings = java.util.HashMap<TestModule, BindingContext>()

        for ((testModule, testFilesInModule) in groupedByModule) {

            val jetFiles = getJetFiles(testFilesInModule, true)

            val oldModule = modules.get(testModule)

            val languageVersionSettings = loadLanguageVersionSettings(testFilesInModule)
            val moduleContext = context.withProject(project).withModule(oldModule)

            val separateModules = groupedByModule.size == 1 && groupedByModule.keys.iterator().next() == null
            val result = analyzeModuleContents(
                    moduleContext, jetFiles, CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(),
                    languageVersionSettings, separateModules
            )
            if (oldModule != result.moduleDescriptor) {
                // For common modules, we use DefaultAnalyzerFacade who creates ModuleDescriptor instances by itself
                // (its API does not support working with a module created beforehand).
                // So, we should replace the old (effectively discarded) module with the new one everywhere in dependencies.
                // TODO: dirty hack, refactor this test so that it doesn't create ModuleDescriptor instances
                modules.put(testModule, result.moduleDescriptor)
                for (module in modules.values) {
                    val it = module.testOnly_AllDependentModules.listIterator()
                    while (it.hasNext()) {
                        if (it.next() == oldModule) {
                            it.set(result.moduleDescriptor)
                        }
                    }
                }
            }

            moduleBindings.put(testModule, result.bindingContext)
            checkAllResolvedCallsAreCompleted(jetFiles, result.bindingContext)
        }

        // We want to always create a test data file (txt) if it was missing,
        // but don't want to skip the following checks in case this one fails
        var exceptionFromLazyResolveLogValidation: Throwable? = null
        if (checkLazyResolveLog) {
            exceptionFromLazyResolveLogValidation = checkLazyResolveLog(lazyOperationsLog, testDataFile)
        }
        else {
            val lazyLogFile = getLazyLogFile(testDataFile)
            TestCase.assertFalse("No lazy log expected, but found: " + lazyLogFile.getAbsolutePath(), lazyLogFile.exists())
        }

        var exceptionFromDescriptorValidation: Throwable? = null
        try {
            val expectedFile = File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".txt")
            validateAndCompareDescriptorWithFile(expectedFile, testFiles, modules)
        }
        catch (e: Throwable) {
            exceptionFromDescriptorValidation = e
        }

        // main checks
        var ok = true

        val actualText = StringBuilder()
        for (testFile in testFiles) {
            val module = testFile.module
            val isCommonModule = modules.get(module).getMultiTargetPlatform() === MultiTargetPlatform.Common
            ok = ok and testFile.getActualText(
                    moduleBindings.get(module), actualText,
                    shouldSkipJvmSignatureDiagnostics(groupedByModule) || isCommonModule
            )
        }

        var exceptionFromDynamicCallDescriptorsValidation: Throwable? = null
        try {
            val expectedFile = File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".dynamic.txt")
            checkDynamicCallDescriptors(expectedFile, testFiles)
        }
        catch (e: Throwable) {
            exceptionFromDynamicCallDescriptorsValidation = e
        }

        KotlinTestUtils.assertEqualsToFile(testDataFile, actualText.toString())

        TestCase.assertTrue("Diagnostics mismatch. See the output above", ok)

        // now we throw a previously found error, if any
        if (exceptionFromDescriptorValidation != null) {
            throw rethrow(exceptionFromDescriptorValidation)
        }
        if (exceptionFromLazyResolveLogValidation != null) {
            throw rethrow(exceptionFromLazyResolveLogValidation)
        }
        if (exceptionFromDynamicCallDescriptorsValidation != null) {
            throw rethrow(exceptionFromDynamicCallDescriptorsValidation)
        }

        performAdditionalChecksAfterDiagnostics(testDataFile, testFiles, groupedByModule, modules, moduleBindings)
    }

    protected fun performAdditionalChecksAfterDiagnostics(
            testDataFile: File,
            testFiles: List<BaseDiagnosticsTest.TestFile>,
            moduleFiles: Map<BaseDiagnosticsTest.TestModule, List<BaseDiagnosticsTest.TestFile>>,
            moduleDescriptors: Map<BaseDiagnosticsTest.TestModule, ModuleDescriptorImpl>,
            moduleBindings: Map<BaseDiagnosticsTest.TestModule, BindingContext>
    ) {
        // To be overridden by diagnostic-like tests.
    }

    private fun loadLanguageVersionSettings(module: List<BaseDiagnosticsTest.TestFile>): LanguageVersionSettings? {
        var result: LanguageVersionSettings? = null
        for (file in module) {
            val current = file.customLanguageVersionSettings
            if (current != null) {
                if (result != null && result != current) {
                    Assert.fail(
                            "More than one file in the module has " + BaseDiagnosticsTest.LANGUAGE_DIRECTIVE + " or " +
                            BaseDiagnosticsTest.API_VERSION_DIRECTIVE + " directive specified. " +
                            "This is not supported. Please move all directives into one file"
                    )
                }
                result = current
            }
        }

        return result
    }

    private fun checkDynamicCallDescriptors(expectedFile: File, testFiles: List<BaseDiagnosticsTest.TestFile>) {
        val serializer = RecursiveDescriptorComparator(RECURSIVE_ALL)

        val actualText = StringBuilder()

        for (testFile in testFiles) {
            val dynamicCallDescriptors = testFile.dynamicCallDescriptors

            for (descriptor in dynamicCallDescriptors) {
                val actualSerialized = serializer.serializeRecursively(descriptor)
                actualText.append(actualSerialized)
            }
        }

        if (actualText.length != 0 || expectedFile.exists()) {
            KotlinTestUtils.assertEqualsToFile(expectedFile, actualText.toString())
        }
    }

    fun shouldSkipJvmSignatureDiagnostics(groupedByModule: Map<BaseDiagnosticsTest.TestModule, List<BaseDiagnosticsTest.TestFile>>): Boolean {
        return groupedByModule.size > 1
    }

    private fun checkLazyResolveLog(lazyOperationsLog: LazyOperationsLog, testDataFile: File): Throwable? {
        var exceptionFromLazyResolveLogValidation: Throwable? = null
        try {
            val expectedFile = getLazyLogFile(testDataFile)

            KotlinTestUtils.assertEqualsToFile(
                    expectedFile,
                    lazyOperationsLog.getText(),
                    HASH_SANITIZER
            )
        }
        catch (e: Throwable) {
            exceptionFromLazyResolveLogValidation = e
        }

        return exceptionFromLazyResolveLogValidation
    }

    private fun getLazyLogFile(testDataFile: File): File {
        return File(FileUtil.getNameWithoutExtension(testDataFile.absolutePath) + ".lazy.log")
    }

    protected fun analyzeModuleContents(
            moduleContext: ModuleContext,
            files: List<KtFile>,
            moduleTrace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings?,
            separateModules: Boolean
    ): AnalysisResult {
        var files = files
        var languageVersionSettings = languageVersionSettings
        val configuration: CompilerConfiguration
        if (languageVersionSettings != null) {
            configuration = environment.configuration.copy()
            configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, languageVersionSettings)
        }
        else {
            configuration = environment.configuration
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
        }

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
                    configuration,
                    { scope -> JvmPackagePartProvider(environment, scope) }
            )
        }

        val moduleDescriptor = moduleContext.module as ModuleDescriptorImpl

        val platform = moduleDescriptor.getMultiTargetPlatform()
        if (platform === MultiTargetPlatform.Common) {

            return DefaultAnalyzerFacade.analyzeFiles(
                    files, moduleDescriptor.name, true,
                    mapOf<ModuleDescriptor.Capability<out Any>, Any>(
                            MultiTargetPlatform.CAPABILITY.to<ModuleDescriptor.Capability<MultiTargetPlatform>, MultiTargetPlatform.Common>(MultiTargetPlatform.Common),
                            MODULE_FILES.to<ModuleDescriptor.Capability<List<KtFile>>, List<KtFile>>(files)
                    )
            ) { info, content ->
                // TODO
                PackagePartProvider.Empty
            }
        }
        else if (platform != null) {
            // TODO: analyze with the correct platform, not always JVM
            files = files.plus<KtFile>(getCommonCodeFilesForPlatformSpecificModule(moduleDescriptor))
        }

        val moduleContentScope = GlobalSearchScope.allScope(moduleContext.project)
        val moduleClassResolver = SingleModuleClassResolver()
        val container = createContainerForTopDownAnalyzerForJvm(
                moduleContext,
                moduleTrace,
                FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
                moduleContentScope,
                LookupTracker.DO_NOTHING,
                JvmPackagePartProvider(environment, moduleContentScope),
                languageVersionSettings,
                moduleClassResolver
        )
        container.initJvmBuiltInsForTopDownAnalysis(moduleDescriptor, languageVersionSettings)
        moduleClassResolver.resolver = container.getService(JavaDescriptorResolver::class.java)

        moduleDescriptor.initialize(CompositePackageFragmentProvider(Arrays.asList<PackageFragmentProvider>(
                container.getService(KotlinCodeAnalyzer::class.java).getPackageFragmentProvider(),
                container.getService(JavaDescriptorResolver::class.java).packageFragmentProvider
        )))

        container.getService(LazyTopDownAnalyzer::class.java).analyzeDeclarations(
                TopDownAnalysisMode.TopLevelDeclarations, files, DataFlowInfo.EMPTY
        )

        return AnalysisResult.success(moduleTrace.bindingContext, moduleDescriptor)
    }

    private fun getCommonCodeFilesForPlatformSpecificModule(moduleDescriptor: ModuleDescriptorImpl): List<KtFile> {
        // We assume that a platform-specific module _implements_ all declarations from common modules which are immediate dependencies.
        // So we collect all sources from such modules to analyze in the platform-specific module as well
        val dependencies = moduleDescriptor.testOnly_AllDependentModules

        // TODO: diagnostics on common code reported during the platform module analysis should be distinguished somehow
        // E.g. "<!JVM:IMPLEMENTATION_WITHOUT_HEADER!>...<!>
        val result = ArrayList<KtFile>(0)
        for (dependency in dependencies) {
            if (dependency.getCapability(MultiTargetPlatform.CAPABILITY) === MultiTargetPlatform.Common) {
                val files = dependency.getCapability(MODULE_FILES) ?: error("MODULE_FILES should have been set for the common module: " + dependency)
                result.addAll(files)
            }
        }

        return result
    }

    private fun validateAndCompareDescriptorWithFile(
            expectedFile: File,
            testFiles: List<BaseDiagnosticsTest.TestFile>,
            modules: Map<BaseDiagnosticsTest.TestModule, ModuleDescriptorImpl>
    ) {
        if (testFiles.any { file -> InTextDirectivesUtils.isDirectiveDefined(file.expectedText, "// SKIP_TXT") }) {
            TestCase.assertFalse(".txt file should not exist if SKIP_TXT directive is used: " + expectedFile, expectedFile.exists())
            return
        }

        val comparator = RecursiveDescriptorComparator(createdAffectedPackagesConfiguration(testFiles, modules.values))

        val isMultiModuleTest = modules.size != 1
        val rootPackageText = StringBuilder()

        val module = modules.keys.sorted().iterator()
        while (module.hasNext()) {
            val moduleDescriptor = modules[module.next()]
            val aPackage = moduleDescriptor.getPackage(FqName.ROOT)
            TestCase.assertFalse(aPackage.isEmpty())

            if (isMultiModuleTest) {
                rootPackageText.append(String.format("// -- Module: %s --\n", moduleDescriptor.getName()))
            }

            val actualSerialized = comparator.serializeRecursively(aPackage)
            rootPackageText.append(actualSerialized)

            if (isMultiModuleTest && module.hasNext()) {
                rootPackageText.append("\n\n")
            }
        }

        val lineCount = StringUtil.getLineBreakCount(rootPackageText)
        assert(lineCount < 1000) {
            "Rendered descriptors of this test take up " + lineCount + " lines. " +
            "Please ensure you don't render JRE contents to the .txt file. " +
            "Such tests are hard to maintain, take long time to execute and are subject to sudden unreviewed changes anyway."
        }

        KotlinTestUtils.assertEqualsToFile(expectedFile, rootPackageText.toString())
    }

    private fun createdAffectedPackagesConfiguration(testFiles: List<BaseDiagnosticsTest.TestFile>, modules: Collection<ModuleDescriptor>): RecursiveDescriptorComparator.Configuration {
        val packagesNames = getTopLevelPackagesFromFileList(getJetFiles(testFiles, false))

        val stepIntoFilter = Predicate<DeclarationDescriptor> { descriptor ->
            val module = DescriptorUtils.getContainingModuleOrNull(descriptor!!)
            if (!modules.contains(module)) return@Predicate false

            if (descriptor is PackageViewDescriptor) {
                val fqName = descriptor.fqName

                if (fqName.isRoot) return@Predicate true

                val firstName = fqName.pathSegments()[0]
                return@Predicate packagesNames.contains(firstName)
            }

            true
        }

        return RECURSIVE.filterRecursion(stepIntoFilter).withValidationStrategy(DescriptorValidator.ValidationVisitor.errorTypesAllowed())
    }

    private fun getTopLevelPackagesFromFileList(files: List<KtFile>): Set<Name> {
        val shortNames = LinkedHashSet<Name>()
        for (file in files) {
            val packageFqNameSegments = file.packageFqName.pathSegments()
            val name = if (packageFqNameSegments.isEmpty()) SpecialNames.ROOT_PACKAGE else packageFqNameSegments[0]
            shortNames.add(name)
        }
        return shortNames
    }

    private fun createModules(
            groupedByModule: Map<BaseDiagnosticsTest.TestModule, List<BaseDiagnosticsTest.TestFile>>,
            storageManager: StorageManager
    ): Map<BaseDiagnosticsTest.TestModule, ModuleDescriptorImpl> {
        val modules = HashMap<BaseDiagnosticsTest.TestModule, ModuleDescriptorImpl>()

        for (testModule in groupedByModule.keys) {
            val module = if (testModule == null)
                createSealedModule(storageManager)
            else
                createModule(testModule.name, storageManager)

            modules.put(testModule, module)
        }

        for (testModule in groupedByModule.keys) {
            if (testModule == null) continue

            val module = modules[testModule]
            val dependencies = ArrayList<ModuleDescriptorImpl>()
            dependencies.add(module)
            for (dependency in testModule.getDependencies()) {
                dependencies.add(modules[dependency])
            }

            dependencies.add(module.builtIns.builtInsModule)
            dependencies.addAll(getAdditionalDependencies(module))
            module.setDependencies(dependencies)
        }

        return modules
    }

    protected fun getAdditionalDependencies(module: ModuleDescriptorImpl): List<ModuleDescriptorImpl> {
        return emptyList()
    }

    protected fun createModule(moduleName: String, storageManager: StorageManager): ModuleDescriptorImpl {
        val nameSuffix = moduleName.substringAfterLast("-", "")
        val platform = if (nameSuffix.isEmpty())
            null
        else if (nameSuffix == "common") MultiTargetPlatform.Common else MultiTargetPlatform.Specific(nameSuffix)
        val capabilities = if (platform == null)
            emptyMap<Any, Any>()
        else
            Collections.singletonMap<ModuleDescriptor.Capability<MultiTargetPlatform>, MultiTargetPlatform>(MultiTargetPlatform.CAPABILITY, platform)
        return ModuleDescriptorImpl(
                Name.special("<$moduleName>"), storageManager, JvmBuiltIns(storageManager),
                if (platform === MultiTargetPlatform.Common) PlatformKind.DEFAULT else PlatformKind.JVM,
                SourceKind.TEST, capabilities
        )
    }

    protected fun createSealedModule(storageManager: StorageManager): ModuleDescriptorImpl {
        val moduleDescriptor = createModule("test-module", storageManager)
        moduleDescriptor.setDependencies(moduleDescriptor, moduleDescriptor.builtIns.builtInsModule)
        return moduleDescriptor
    }

    private fun checkAllResolvedCallsAreCompleted(jetFiles: List<KtFile>, bindingContext: BindingContext) {
        for (file in jetFiles) {
            if (!AnalyzingUtils.getSyntaxErrorRanges(file).isEmpty()) {
                return
            }
        }

        val resolvedCallsEntries = bindingContext.getSliceContents(BindingContext.RESOLVED_CALL)
        for (entry in resolvedCallsEntries.entries) {
            val element = entry.key.callElement
            val resolvedCall = entry.value

            val lineAndColumn = DiagnosticUtils.getLineAndColumnInPsiFile(element.containingFile, element.textRange)

            TestCase.assertTrue("Resolved call for '" + element.text + "'" + lineAndColumn + " is not completed",
                                (resolvedCall as MutableResolvedCall<*>).isCompleted)
        }

        checkResolvedCallsInDiagnostics(bindingContext)
    }

    private fun checkResolvedCallsInDiagnostics(bindingContext: BindingContext) {
        val diagnosticsStoringResolvedCalls1 = Sets.newHashSet<DiagnosticFactory1<PsiElement, Collection<ResolvedCall<*>>>>(
                OVERLOAD_RESOLUTION_AMBIGUITY, NONE_APPLICABLE, CANNOT_COMPLETE_RESOLVE, UNRESOLVED_REFERENCE_WRONG_RECEIVER,
                ASSIGN_OPERATOR_AMBIGUITY, ITERATOR_AMBIGUITY)
        val diagnosticsStoringResolvedCalls2 = Sets.newHashSet<DiagnosticFactory2<KtExpression, out Comparable<out Comparable<*>>, Collection<ResolvedCall<*>>>>(
                COMPONENT_FUNCTION_AMBIGUITY, DELEGATE_SPECIAL_FUNCTION_AMBIGUITY, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE)
        val diagnostics = bindingContext.diagnostics
        for (diagnostic in diagnostics) {
            val factory = diagnostic.factory

            if (diagnosticsStoringResolvedCalls1.contains(factory)) {
                assertResolvedCallsAreCompleted(
                        diagnostic, DiagnosticFactory.cast(diagnostic, diagnosticsStoringResolvedCalls1).a)
            }

            if (diagnosticsStoringResolvedCalls2.contains(factory)) {
                assertResolvedCallsAreCompleted(
                        diagnostic,
                        DiagnosticFactory.cast<DiagnosticWithParameters2<KtExpression, out Comparable<out Comparable<*>>, Collection<ResolvedCall<*>>>>(diagnostic, diagnosticsStoringResolvedCalls2).b)
            }
        }
    }

    private fun assertResolvedCallsAreCompleted(
            diagnostic: Diagnostic, resolvedCalls: Collection<ResolvedCall<*>>
    ) {
        var allCallsAreCompleted = true
        for (resolvedCall in resolvedCalls) {
            if (!(resolvedCall as MutableResolvedCall<*>).isCompleted) {
                allCallsAreCompleted = false
            }
        }

        val element = diagnostic.psiElement
        val lineAndColumn = DiagnosticUtils.getLineAndColumnInPsiFile(element.containingFile, element.textRange)

        TestCase.assertTrue("Resolved calls stored in " + diagnostic.factory.name + "\n" +
                            "for '" + element.text + "'" + lineAndColumn + " are not completed",
                            allCallsAreCompleted)
    }

    companion object {
        private val HASH_SANITIZER = fun(s: String): String {
            return s.replace("@(\\d)+".toRegex(), "")
        }

        private val MODULE_FILES = ModuleDescriptor.Capability<List<KtFile>>("")
    }
}
