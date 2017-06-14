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

package org.jetbrains.kotlin.idea.debugger;

import com.google.common.collect.Lists;
import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.debugger.impl.DescriptorTestCase;
import com.intellij.debugger.impl.OutputChecker;
import com.intellij.execution.ExecutionTestCase;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.xdebugger.XDebugSession;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.classes.FakeLightClassForFileOfPackage;
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.MockLibraryUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.jetbrains.kotlin.test.util.JetTestUtilsKt;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.PathUtil;
import org.junit.Assert;
import org.junit.ComparisonFailure;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class KotlinDebuggerTestCase extends DescriptorTestCase {
    private static final String TINY_APP = PluginTestCaseBase.getTestDataPathBase() + "/debugger/tinyApp";
    private static final File TINY_APP_SRC = new File(TINY_APP, "src");
    private static boolean IS_TINY_APP_COMPILED = false;

    // Caches are auto-invalidated when file modification in TINY_APP_SRC detected (through File.lastModified()).
    // LOCAL_CACHE_DIR removing can be used to force caches invalidating as well.
    private static final boolean LOCAL_CACHE_REUSE = true;

    private static final File LOCAL_CACHE_DIR = new File("out/debuggerTinyApp");
    private static final File LOCAL_CACHE_JAR_DIR = new File(LOCAL_CACHE_DIR, "jar");
    private static final File LOCAL_CACHE_APP_DIR = new File(LOCAL_CACHE_DIR, "app");
    private static final File LOCAL_CACHE_LAST_MODIFIED_FILE = new File(LOCAL_CACHE_DIR, "lastModified.txt");

    private static File CUSTOM_LIBRARY_JAR;
    private static final File CUSTOM_LIBRARY_SOURCES =
            new File(PluginTestCaseBase.getTestDataPathBase() + "/debugger/customLibraryForTinyApp");

    protected static final String KOTLIN_LIBRARY_NAME = "KotlinLibrary";
    private static final String CUSTOM_LIBRARY_NAME = "CustomLibrary";

    @Override
    protected OutputChecker initOutputChecker() {
        return new KotlinOutputChecker(
                this.getClass().getAnnotation(TestMetadata.class).value(), getTestAppPath(), getAppOutputPath());
    }

    @NotNull
    @Override
    protected String getTestAppPath() {
        return TINY_APP;
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @Override
    protected void setUp() throws Exception {
        if (LOCAL_CACHE_REUSE) {
            boolean localCacheRebuild = false;

            if (LOCAL_CACHE_DIR.exists()) {
                if (isLocalCacheOutdated()) {
                    System.out.println("-- Local caches outdated --");
                    deleteLocalCacheDirectory(true);
                    localCacheRebuild = true;
                }
            }
            else {
                localCacheRebuild = true;
            }

            overrideTempOutputDirectory();
            CUSTOM_LIBRARY_JAR = new File(LOCAL_CACHE_DIR, "debuggerCustomLibrary.jar");
            IS_TINY_APP_COMPILED = !localCacheRebuild;
        }

        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory());
        if (DexLikeBytecodePatchKt.needDexPatch(getTestName(true))) {
            NoStrataPositionManagerHelperKt.setEmulateDexDebugInTests(true);
        }
        super.setUp();
    }

    private static void deleteLocalCacheDirectory(boolean assertDeleteSuccess) {
        System.out.println("-- Remove local cache directory --");
        boolean deleteResult = FilesKt.deleteRecursively(LOCAL_CACHE_DIR);
        if (assertDeleteSuccess) {
            Assert.assertTrue("Failed to delete local cache!", deleteResult);
        }
    }

    private static long cachedDataTimeStamp() {
        File testDataLastModifiedFile = JetTestUtilsKt.findLastModifiedFile(
                TINY_APP_SRC,
                file -> FilesKt.getExtension(file).equals("out") || file.isDirectory()
        );

        File distLibLastModifiedFile = JetTestUtilsKt.findLastModifiedFile(
                PathUtil.getKotlinPathsForDistDirectory().getLibPath(), file -> false);

        return Math.max(testDataLastModifiedFile.lastModified(), distLibLastModifiedFile.lastModified());
    }

    private static boolean isLocalCacheOutdated() {
        if (!LOCAL_CACHE_LAST_MODIFIED_FILE.exists()) return true;

        String text;
        try {
            text = FileUtil.loadFile(LOCAL_CACHE_LAST_MODIFIED_FILE);
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }

        long cachedFor = Long.parseLong(text);
        long currentLastDate = cachedDataTimeStamp();

        return currentLastDate != cachedFor;
    }

    private static void overrideTempOutputDirectory() {
        try {
            Field ourOutputRootField = ExecutionTestCase.class.getDeclaredField("ourOutputRoot");
            ourOutputRootField.setAccessible(true);

            if (!LOCAL_CACHE_DIR.exists()) {
                boolean result =
                        LOCAL_CACHE_DIR.mkdir() &&
                        LOCAL_CACHE_JAR_DIR.mkdir() &&
                        LOCAL_CACHE_APP_DIR.mkdir();

                Assert.assertTrue("Failure on local cache directories creation", result);

                boolean createFileResult = LOCAL_CACHE_LAST_MODIFIED_FILE.createNewFile();
                Assert.assertTrue("Failure on " + LOCAL_CACHE_LAST_MODIFIED_FILE.getName() + " creation", createFileResult);

                long lastModificationDate = cachedDataTimeStamp();
                FileUtil.writeToFile(LOCAL_CACHE_LAST_MODIFIED_FILE, Long.toString(lastModificationDate));
            }

            ourOutputRootField.set(null, LOCAL_CACHE_APP_DIR);
        }
        catch (NoSuchFieldException | IOException | IllegalAccessException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    private static void configureLibrary(
            @NotNull ModifiableRootModel model,
            @NotNull String libraryName,
            @NotNull File classes,
            @NotNull File sources
    ) {
        NewLibraryEditor customLibEditor = new NewLibraryEditor();
        customLibEditor.setName(libraryName);

        customLibEditor.addRoot(VfsUtil.getUrlForLibraryRoot(classes), OrderRootType.CLASSES);
        customLibEditor.addRoot(VfsUtil.getUrlForLibraryRoot(sources), OrderRootType.SOURCES);

        ConfigLibraryUtil.INSTANCE.addLibrary(customLibEditor, model, null);
    }

    @Override
    protected void tearDown() throws Exception {
        if (DexLikeBytecodePatchKt.needDexPatch(getTestName(true))) {
            NoStrataPositionManagerHelperKt.setEmulateDexDebugInTests(false);
        }

        EdtTestUtil.runInEdtAndWait((ThrowableRunnable<Throwable>) () -> {
            ConfigLibraryUtil.INSTANCE.removeLibrary(getModule(), CUSTOM_LIBRARY_NAME);
            ConfigLibraryUtil.INSTANCE.removeLibrary(getModule(), KOTLIN_LIBRARY_NAME);
        });

        super.tearDown();
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory());
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @Override
    protected void setUpModule() {
        super.setUpModule();

        IdeaTestUtil.setModuleLanguageLevel(myModule, LanguageLevel.JDK_1_6);

        String outputDirPath = getAppOutputPath();
        File outDir = new File(outputDirPath);

        if (!IS_TINY_APP_COMPILED) {
            try {
                String modulePath = getTestAppPath();

                //noinspection ConstantConditions
                File jarDir = LOCAL_CACHE_REUSE ? LOCAL_CACHE_DIR : KotlinTestUtils.tmpDir("debuggerCustomLibrary");

                CUSTOM_LIBRARY_JAR = MockLibraryUtil.compileLibraryToJar(CUSTOM_LIBRARY_SOURCES.getPath(), jarDir, "debuggerCustomLibrary");

                String sourcesDir = modulePath + File.separator + "src";

                MockLibraryUtil.compileKotlin(sourcesDir, outDir, CUSTOM_LIBRARY_JAR.getPath());

                List<String> options =
                        Arrays.asList("-d", outputDirPath, "-classpath", ForTestCompileRuntime.runtimeJarForTests().getPath(), "-g");
                KotlinTestUtils.compileJavaFiles(findJavaFiles(new File(sourcesDir)), options);

                DexLikeBytecodePatchKt.patchDexTests(outDir);

                IS_TINY_APP_COMPILED = true;
            }
            catch (Throwable e) {
                deleteLocalCacheDirectory(false);
                throw new RuntimeException(e);
            }
        }

        CompilerUtil.refreshOutputRoots(Lists.newArrayList(outputDirPath));

        ApplicationManager.getApplication().runWriteAction(() -> {
            ModifiableRootModel model = ModuleRootManager.getInstance(myModule).getModifiableModel();
            configureLibrary(model, CUSTOM_LIBRARY_NAME, CUSTOM_LIBRARY_JAR, CUSTOM_LIBRARY_SOURCES);
            configureLibrary(model, KOTLIN_LIBRARY_NAME, ForTestCompileRuntime.runtimeJarForTests(), new File("libraries/stdlib/src"));
            model.commit();
        });

        if (!outDir.exists()) {
            deleteLocalCacheDirectory(false);
            Assert.fail("Output directory for module wasn't created: " + outDir.getAbsolutePath());
        }
    }

    private static List<File> findJavaFiles(@NotNull File directory) {
        List<File> result = new ArrayList<>();
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        result.addAll(findJavaFiles(file));
                    }
                    else if (file.getName().endsWith(".java")) {
                        result.add(file);
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected JavaParameters createJavaParameters(String mainClass) {
        JavaParameters parameters = super.createJavaParameters(mainClass);
        parameters.getClassPath().add(ForTestCompileRuntime.runtimeJarForTests());
        parameters.getClassPath().add(CUSTOM_LIBRARY_JAR);
        return parameters;
    }

    @Override
    protected void createBreakpoints(String className) {
        PsiClass[] psiClasses = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiClass[]>) () -> JavaPsiFacade.getInstance(myProject)
                        .findClasses(className, GlobalSearchScope.allScope(myProject)));

        for (PsiClass psiClass : psiClasses) {
            if (psiClass instanceof KtLightClassForFacade) {
                Collection<KtFile> files = ((KtLightClassForFacade) psiClass).getFiles();
                for (KtFile jetFile : files) {
                    createBreakpoints(jetFile);
                }
            }
            else if (psiClass instanceof FakeLightClassForFileOfPackage) {
                // skip, because we already create breakpoints using KotlinLightClassForPackage
            }
            else {
                createBreakpoints(psiClass.getContainingFile());
            }
        }
    }

    @SuppressWarnings("MethodMayBeStatic")
    protected void createDebugProcess(@NotNull String path) throws Exception {
        File file = new File(path);
        //noinspection ConstantConditions
        FileBasedIndex.getInstance().requestReindex(VfsUtil.findFileByIoFile(file, true));
        String packageName = file.getName().replace(".kt", "");
        FqName packageFQN = new FqName(packageName);
        String mainClassName = PackagePartClassUtils.getPackagePartFqName(packageFQN, file.getName()).asString();
        createLocalProcess(mainClassName);
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.fullJdk();
    }

    @Override
    protected void checkTestOutput() throws Exception {
        if (KotlinTestUtils.isAllFilesPresentTest(getTestName(false))) {
            return;
        }

        try {
            super.checkTestOutput();
        }
        catch (ComparisonFailure e) {
            KotlinTestUtils.assertEqualsToFile(
                    new File(this.getClass().getAnnotation(TestMetadata.class).value(), getTestName(true) + ".out"),
                    e.getActual());
        }
    }

    @Override
    public Object getData(String dataId) {
        if (XDebugSession.DATA_KEY.is(dataId)) {
            return myDebuggerSession == null ? null : myDebuggerSession.getXDebugSession();
        }
        return super.getData(dataId);
    }
}
