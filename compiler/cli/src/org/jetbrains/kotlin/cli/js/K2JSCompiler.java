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

package org.jetbrains.kotlin.cli.js;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.SmartList;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants;
import org.jetbrains.kotlin.cli.common.config.ContentRootsKt;
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.MessageUtil;
import org.jetbrains.kotlin.cli.common.output.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker;
import org.jetbrains.kotlin.incremental.components.LookupTracker;
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider;
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer;
import org.jetbrains.kotlin.incremental.js.TranslationResultValue;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.js.config.JSConfigurationKeys;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding;
import org.jetbrains.kotlin.js.facade.K2JSTranslator;
import org.jetbrains.kotlin.js.facade.MainCallParameters;
import org.jetbrains.kotlin.js.facade.TranslationResult;
import org.jetbrains.kotlin.js.facade.TranslationUnit;
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException;
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver;
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.serialization.js.ModuleKind;
import org.jetbrains.kotlin.utils.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR;
import static org.jetbrains.kotlin.cli.common.ExitCode.OK;
import static org.jetbrains.kotlin.cli.common.UtilsKt.checkKotlinPackageUsage;
import static org.jetbrains.kotlin.cli.common.UtilsKt.getLibraryFromHome;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*;

public class K2JSCompiler extends CLICompiler<K2JSCompilerArguments> {
    private static final Map<String, ModuleKind> moduleKindMap = new HashMap<>();
    private static final Map<String, SourceMapSourceEmbedding> sourceMapContentEmbeddingMap = new LinkedHashMap<>();

    private K2JsIrCompiler irCompiler = null;

    @NotNull
    private K2JsIrCompiler getIrCompiler() {
        if (irCompiler == null)
            irCompiler = new K2JsIrCompiler();
        return irCompiler;
    }

    static {
        moduleKindMap.put(K2JsArgumentConstants.MODULE_PLAIN, ModuleKind.PLAIN);
        moduleKindMap.put(K2JsArgumentConstants.MODULE_COMMONJS, ModuleKind.COMMON_JS);
        moduleKindMap.put(K2JsArgumentConstants.MODULE_AMD, ModuleKind.AMD);
        moduleKindMap.put(K2JsArgumentConstants.MODULE_UMD, ModuleKind.UMD);

        sourceMapContentEmbeddingMap.put(K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_ALWAYS, SourceMapSourceEmbedding.ALWAYS);
        sourceMapContentEmbeddingMap.put(K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_NEVER, SourceMapSourceEmbedding.NEVER);
        sourceMapContentEmbeddingMap.put(K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING, SourceMapSourceEmbedding.INLINING);
    }

    public static void main(String... args) {
        doMain(new K2JSCompiler(), args);
    }

    private final K2JSCompilerPerformanceManager performanceManager = new K2JSCompilerPerformanceManager();

    @NotNull
    @Override
    public K2JSCompilerArguments createArguments() {
        return new K2JSCompilerArguments();
    }

    @NotNull
    private static TranslationResult translate(
            @NotNull JsConfig.Reporter reporter,
            @NotNull List<KtFile> allKotlinFiles,
            @NotNull JsAnalysisResult jsAnalysisResult,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull JsConfig config
    ) throws TranslationException {
        K2JSTranslator translator = new K2JSTranslator(config);
        IncrementalDataProvider incrementalDataProvider = config.getConfiguration().get(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER);
        if (incrementalDataProvider != null) {
            Map<File, KtFile> nonCompiledSources = new HashMap<>(allKotlinFiles.size());
            for (KtFile ktFile : allKotlinFiles) {
                nonCompiledSources.put(VfsUtilCore.virtualToIoFile(ktFile.getVirtualFile()), ktFile);
            }

            Map<File, TranslationResultValue> compiledParts = incrementalDataProvider.getCompiledPackageParts();

            File[] allSources = new File[compiledParts.size() + allKotlinFiles.size()];
            int i = 0;
            for (File file : compiledParts.keySet()) {
                allSources[i++] = file;
            }
            for (File file : nonCompiledSources.keySet()) {
                allSources[i++] = file;
            }
            Arrays.sort(allSources);

            List<TranslationUnit> translationUnits = new ArrayList<>();
            for (i = 0; i < allSources.length; i++) {
                KtFile nonCompiled = nonCompiledSources.get(allSources[i]);
                if (nonCompiled != null) {
                    translationUnits.add(new TranslationUnit.SourceFile(nonCompiled));
                }
                else {
                    TranslationResultValue translatedValue = compiledParts.get(allSources[i]);
                    translationUnits.add(new TranslationUnit.BinaryAst(translatedValue.getBinaryAst(), translatedValue.getInlineData()));
                }
            }
            return translator.translateUnits(reporter, translationUnits, mainCallParameters, jsAnalysisResult);
        }

        CollectionsKt.sortBy(allKotlinFiles, ktFile -> VfsUtilCore.virtualToIoFile(ktFile.getVirtualFile()));
        return translator.translate(reporter, allKotlinFiles, mainCallParameters, jsAnalysisResult);
    }

    @NotNull
    @Override
    protected ExitCode doExecute(
            @NotNull K2JSCompilerArguments arguments,
            @NotNull CompilerConfiguration configuration,
            @NotNull Disposable rootDisposable,
            @Nullable KotlinPaths paths
    ) {
        MessageCollector messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY);

        if (arguments.getIrBackend()) {
            return getIrCompiler().doExecute(arguments, configuration, rootDisposable, paths);
        }

        if (arguments.getFreeArgs().isEmpty() && !IncrementalCompilation.isEnabledForJs()) {
            if (arguments.getVersion()) {
                return OK;
            }
            messageCollector.report(ERROR, "Specify at least one source file or directory", null);
            return COMPILATION_ERROR;
        }

        ExitCode pluginLoadResult =
                PluginCliParser.loadPluginsSafe(arguments.getPluginClasspaths(), arguments.getPluginOptions(), configuration);
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult;

        configuration.put(JSConfigurationKeys.LIBRARIES, configureLibraries(arguments, paths, messageCollector));

        String[] commonSourcesArray = arguments.getCommonSources();
        Set<String> commonSources = commonSourcesArray == null ? Collections.emptySet() : SetsKt.setOf(commonSourcesArray);
        for (String arg : arguments.getFreeArgs()) {
            ContentRootsKt.addKotlinSourceRoot(configuration, arg, commonSources.contains(arg));
        }

        KotlinCoreEnvironment environmentForJS =
                KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES);

        Project project = environmentForJS.getProject();
        List<KtFile> sourcesFiles = environmentForJS.getSourceFiles();

        environmentForJS.getConfiguration().put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.getAllowKotlinPackage());

        if (!checkKotlinPackageUsage(environmentForJS, sourcesFiles)) return ExitCode.COMPILATION_ERROR;

        if (arguments.getOutputFile() == null) {
            messageCollector.report(ERROR, "Specify output file via -output", null);
            return ExitCode.COMPILATION_ERROR;
        }

        if (messageCollector.hasErrors()) {
            return ExitCode.COMPILATION_ERROR;
        }

        if (sourcesFiles.isEmpty() && !IncrementalCompilation.isEnabledForJs()) {
            messageCollector.report(ERROR, "No source files", null);
            return COMPILATION_ERROR;
        }

        if (arguments.getVerbose()) {
            reportCompiledSourcesList(messageCollector, sourcesFiles);
        }

        File outputFile = new File(arguments.getOutputFile());

        configuration.put(CommonConfigurationKeys.MODULE_NAME, FileUtil.getNameWithoutExtension(outputFile));

        JsConfig config = new JsConfig(project, configuration);
        JsConfig.Reporter reporter = new JsConfig.Reporter() {
            @Override
            public void error(@NotNull String message) {
                messageCollector.report(ERROR, message, null);
            }

            @Override
            public void warning(@NotNull String message) {
                messageCollector.report(STRONG_WARNING, message, null);
            }
        };
        if (config.checkLibFilesAndReportErrors(reporter)) {
            return COMPILATION_ERROR;
        }

        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(
                messageCollector, CommonConfigurationKeysKt.getLanguageVersionSettings(configuration)
        );
        analyzerWithCompilerReport.analyzeAndReport(sourcesFiles, () -> TopDownAnalyzerFacadeForJS.analyzeFiles(sourcesFiles, config));
        if (analyzerWithCompilerReport.hasErrors()) {
            return COMPILATION_ERROR;
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        AnalysisResult analysisResult = analyzerWithCompilerReport.getAnalysisResult();
        assert analysisResult instanceof JsAnalysisResult : "analysisResult should be instance of JsAnalysisResult, but " + analysisResult;
        JsAnalysisResult jsAnalysisResult = (JsAnalysisResult) analysisResult;

        File outputPrefixFile = null;
        if (arguments.getOutputPrefix() != null) {
            outputPrefixFile = new File(arguments.getOutputPrefix());
            if (!outputPrefixFile.exists()) {
                messageCollector.report(ERROR, "Output prefix file '" + arguments.getOutputPrefix() + "' not found", null);
                return ExitCode.COMPILATION_ERROR;
            }
        }

        File outputPostfixFile = null;
        if (arguments.getOutputPostfix() != null) {
            outputPostfixFile = new File(arguments.getOutputPostfix());
            if (!outputPostfixFile.exists()) {
                messageCollector.report(ERROR, "Output postfix file '" + arguments.getOutputPostfix() + "' not found", null);
                return ExitCode.COMPILATION_ERROR;
            }
        }

        File outputDir = outputFile.getParentFile();
        if (outputDir == null) {
            outputDir = outputFile.getAbsoluteFile().getParentFile();
        }
        try {
            config.getConfiguration().put(JSConfigurationKeys.OUTPUT_DIR, outputDir.getCanonicalFile());
        }
        catch (IOException e) {
            messageCollector.report(ERROR, "Could not resolve output directory", null);
            return ExitCode.COMPILATION_ERROR;
        }

        if (config.getConfiguration().getBoolean(JSConfigurationKeys.SOURCE_MAP)) {
            checkDuplicateSourceFileNames(messageCollector, sourcesFiles, config);
        }

        MainCallParameters mainCallParameters = createMainCallParameters(arguments.getMain());
        TranslationResult translationResult;

        try {
            //noinspection unchecked
            translationResult = translate(reporter, sourcesFiles, jsAnalysisResult, mainCallParameters, config);
        }
        catch (Exception e) {
            throw ExceptionUtilsKt.rethrow(e);
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        AnalyzerWithCompilerReport.Companion.reportDiagnostics(translationResult.getDiagnostics(), messageCollector);

        if (!(translationResult instanceof TranslationResult.Success)) return ExitCode.COMPILATION_ERROR;

        TranslationResult.Success successResult = (TranslationResult.Success) translationResult;
        OutputFileCollection outputFiles = successResult.getOutputFiles(outputFile, outputPrefixFile, outputPostfixFile);

        if (outputFile.isDirectory()) {
            messageCollector.report(ERROR, "Cannot open output file '" + outputFile.getPath() + "': is a directory", null);
            return ExitCode.COMPILATION_ERROR;
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        OutputUtilsKt.writeAll(outputFiles, outputDir, messageCollector,
                               configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES));

        return OK;
    }

    private static void checkDuplicateSourceFileNames(
            @NotNull MessageCollector log,
            @NotNull List<KtFile> sourceFiles,
            @NotNull JsConfig config
    ) {
        if (config.getSourceMapRoots().isEmpty()) return;

        SourceFilePathResolver pathResolver = SourceFilePathResolver.create(config);
        Map<String, String> pathMap = new HashMap<>();
        Set<String> duplicatePaths = new HashSet<>();

        try {
            for (KtFile sourceFile : sourceFiles) {
                String path = sourceFile.getVirtualFile().getPath();
                String relativePath = pathResolver.getPathRelativeToSourceRoots(new File(sourceFile.getVirtualFile().getPath()));

                String existingPath = pathMap.get(relativePath);
                if (existingPath != null) {
                    if (duplicatePaths.add(relativePath)) {
                        log.report(WARNING, "There are files with same path '" + relativePath + "', relative to source roots: " +
                                            "'" + path + "' and '" + existingPath + "'. " +
                                            "This will likely cause problems with debugger", null);
                    }
                }
                else {
                    pathMap.put(relativePath, path);
                }
            }
        }
        catch (IOException e) {
            log.report(ERROR, "IO error occurred validating source path:\n" + ExceptionUtil.getThrowableText(e), null);
        }
    }

    private static void reportCompiledSourcesList(@NotNull MessageCollector messageCollector, @NotNull List<KtFile> sourceFiles) {
        Iterable<String> fileNames = CollectionsKt.map(sourceFiles, file -> {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                return MessageUtil.virtualFileToPath(virtualFile);
            }
            return file.getName() + " (no virtual file)";
        });
        messageCollector.report(LOGGING, "Compiling source files: " + StringsKt.join(fileNames, ", "), null);
    }

    @Override
    protected void setupPlatformSpecificArgumentsAndServices(
            @NotNull CompilerConfiguration configuration, @NotNull K2JSCompilerArguments arguments,
            @NotNull Services services
    ) {
        if (arguments.getIrBackend()) {
            getIrCompiler().setupPlatformSpecificArgumentsAndServices(configuration, arguments, services);
            return;
        }

        MessageCollector messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY);

        if (arguments.getTarget() != null) {
            assert "v5".equals(arguments.getTarget()) : "Unsupported ECMA version: " + arguments.getTarget();
        }
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.defaultVersion());

        if (arguments.getSourceMap()) {
            configuration.put(JSConfigurationKeys.SOURCE_MAP, true);
            if (arguments.getSourceMapPrefix() != null) {
                configuration.put(JSConfigurationKeys.SOURCE_MAP_PREFIX, arguments.getSourceMapPrefix());
            }

            String sourceMapSourceRoots = arguments.getSourceMapBaseDirs();
            if (sourceMapSourceRoots == null && StringUtil.isNotEmpty(arguments.getSourceMapPrefix())) {
                sourceMapSourceRoots = calculateSourceMapSourceRoot(messageCollector, arguments);
            }

            if (sourceMapSourceRoots != null) {
                List<String> sourceMapSourceRootList = StringUtil.split(sourceMapSourceRoots, File.pathSeparator);
                configuration.put(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, sourceMapSourceRootList);
            }
        }
        else {
            if (arguments.getSourceMapPrefix() != null) {
                messageCollector.report(WARNING, "source-map-prefix argument has no effect without source map", null);
            }
            if (arguments.getSourceMapBaseDirs() != null) {
                messageCollector.report(WARNING, "source-map-source-root argument has no effect without source map", null);
            }
        }
        if (arguments.getMetaInfo()) {
            configuration.put(JSConfigurationKeys.META_INFO, true);
        }

        configuration.put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, arguments.getTypedArrays());

        configuration.put(JSConfigurationKeys.FRIEND_PATHS_DISABLED, arguments.getFriendModulesDisabled());

        if (!arguments.getFriendModulesDisabled() && arguments.getFriendModules() != null) {
            List<String> friendPaths = ArraysKt.filterNot(arguments.getFriendModules().split(File.pathSeparator), String::isEmpty);
            configuration.put(JSConfigurationKeys.FRIEND_PATHS, friendPaths);
        }

        String moduleKindName = arguments.getModuleKind();
        ModuleKind moduleKind = moduleKindName != null ? moduleKindMap.get(moduleKindName) : ModuleKind.PLAIN;
        if (moduleKind == null) {
            messageCollector.report(
                    ERROR, "Unknown module kind: " + moduleKindName + ". Valid values are: plain, amd, commonjs, umd", null
            );
            moduleKind = ModuleKind.PLAIN;
        }
        configuration.put(JSConfigurationKeys.MODULE_KIND, moduleKind);

        IncrementalDataProvider incrementalDataProvider = services.get(IncrementalDataProvider.class);
        if (incrementalDataProvider != null) {
            configuration.put(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER, incrementalDataProvider);
        }

        IncrementalResultsConsumer incrementalResultsConsumer = services.get(IncrementalResultsConsumer.class);
        if (incrementalResultsConsumer != null) {
            configuration.put(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER, incrementalResultsConsumer);
        }

        LookupTracker lookupTracker = services.get(LookupTracker.class);
        if (lookupTracker != null) {
            configuration.put(CommonConfigurationKeys.LOOKUP_TRACKER, lookupTracker);
        }

        ExpectActualTracker expectActualTracker = services.get(ExpectActualTracker.class);
        if (expectActualTracker != null) {
            configuration.put(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER, expectActualTracker);
        }

        String sourceMapEmbedContentString = arguments.getSourceMapEmbedSources();
        SourceMapSourceEmbedding sourceMapContentEmbedding = sourceMapEmbedContentString != null ?
                                                             sourceMapContentEmbeddingMap.get(sourceMapEmbedContentString) :
                                                             SourceMapSourceEmbedding.INLINING;
        if (sourceMapContentEmbedding == null) {
            String message = "Unknown source map source embedding mode: " + sourceMapEmbedContentString + ". Valid values are: " +
                             StringUtil.join(sourceMapContentEmbeddingMap.keySet(), ", ");
            messageCollector.report(ERROR, message, null);
            sourceMapContentEmbedding = SourceMapSourceEmbedding.INLINING;
        }
        configuration.put(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, sourceMapContentEmbedding);

        if (!arguments.getSourceMap() && sourceMapEmbedContentString != null) {
            messageCollector.report(WARNING, "source-map-embed-sources argument has no effect without source map", null);
        }
    }

    @NotNull
    private static List<String> configureLibraries(
            @NotNull K2JSCompilerArguments arguments,
            @Nullable KotlinPaths paths,
            @NotNull MessageCollector messageCollector
    ) {
        List<String> libraries = new SmartList<>();
        if (!arguments.getNoStdlib()) {
            File stdlibJar = getLibraryFromHome(
                    paths, KotlinPaths::getJsStdLibJarPath, PathUtil.JS_LIB_JAR_NAME, messageCollector, "'-no-stdlib'");
            if (stdlibJar != null) {
                libraries.add(stdlibJar.getAbsolutePath());
            }
        }

        if (arguments.getLibraries() != null) {
            libraries.addAll(ArraysKt.filterNot(arguments.getLibraries().split(File.pathSeparator), String::isEmpty));
        }
        return libraries;
    }

    @NotNull
    private static String calculateSourceMapSourceRoot(
            @NotNull MessageCollector messageCollector,
            @NotNull K2JSCompilerArguments arguments
    ) {
        File commonPath = null;
        List<File> pathToRoot = new ArrayList<>();
        Map<File, Integer> pathToRootIndexes = new HashMap<>();

        try {
            for (String path : arguments.getFreeArgs()) {
                File file = new File(path).getCanonicalFile();
                if (commonPath == null) {
                    commonPath = file;

                    while (file != null) {
                        pathToRoot.add(file);
                        file = file.getParentFile();
                    }
                    Collections.reverse(pathToRoot);

                    for (int i = 0; i < pathToRoot.size(); ++i) {
                        pathToRootIndexes.put(pathToRoot.get(i), i);
                    }
                }
                else {
                    while (file != null) {
                        Integer existingIndex = pathToRootIndexes.get(file);
                        if (existingIndex != null) {
                            existingIndex = Math.min(existingIndex, pathToRoot.size() - 1);
                            pathToRoot.subList(existingIndex + 1, pathToRoot.size()).clear();
                            commonPath = pathToRoot.get(pathToRoot.size() - 1);
                            break;
                        }
                        file = file.getParentFile();
                    }
                    if (file == null) {
                        break;
                    }
                }
            }
        }
        catch (IOException e) {
            String text = ExceptionUtil.getThrowableText(e);
            messageCollector.report(CompilerMessageSeverity.ERROR, "IO error occurred calculating source root:\n" + text, null);
            return ".";
        }

        return commonPath != null ? commonPath.getPath() : ".";
    }

    @NotNull
    @Override
    protected CommonCompilerPerformanceManager getPerformanceManager() {
        return performanceManager;
    }

    private static MainCallParameters createMainCallParameters(String main) {
        if (K2JsArgumentConstants.NO_CALL.equals(main)) {
            return MainCallParameters.noCall();
        }
        else {
            return MainCallParameters.mainWithoutArguments();
        }
    }

    @NotNull
    @Override
    public String executableScriptFileName() {
        return "kotlinc-js";
    }

    @NotNull
    @Override
    protected BinaryVersion createMetadataVersion(@NotNull int[] versionArray) {
        return new JsMetadataVersion(versionArray);
    }

    private static final class K2JSCompilerPerformanceManager extends CommonCompilerPerformanceManager {
        public K2JSCompilerPerformanceManager() {
            super("Kotlin to JS Compiler");
        }
    }
}
