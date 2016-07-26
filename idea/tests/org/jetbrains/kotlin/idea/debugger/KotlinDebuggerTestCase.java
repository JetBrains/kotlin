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

import com.intellij.debugger.impl.DescriptorTestCase;
import com.intellij.debugger.impl.OutputChecker;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import com.intellij.openapi.util.Computable;
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
import org.junit.ComparisonFailure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class KotlinDebuggerTestCase extends DescriptorTestCase {
    protected static final String TINY_APP = PluginTestCaseBase.getTestDataPathBase() + "/debugger/tinyApp";
    private static boolean IS_TINY_APP_COMPILED = false;

    private static File CUSTOM_LIBRARY_JAR;
    private static final File CUSTOM_LIBRARY_SOURCES = new File(PluginTestCaseBase.getTestDataPathBase() + "/debugger/customLibraryForTinyApp");

    protected static final String KOTLIN_LIBRARY_NAME = "KotlinLibrary";
    protected static final String CUSTOM_LIBRARY_NAME = "CustomLibrary";

    @Override
    protected OutputChecker initOutputChecker() {
        return new KotlinOutputChecker(getTestAppPath(), getAppOutputPath());
    }

    @NotNull
    @Override
    protected String getTestAppPath() {
        return TINY_APP;
    }

    @Override
    protected void setUp() throws Exception {
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory());
        super.setUp();
    }

    private static void configureLibrary(@NotNull ModifiableRootModel model, @NotNull String libraryName, @NotNull File classes, @NotNull File sources) {
        NewLibraryEditor customLibEditor = new NewLibraryEditor();
        customLibEditor.setName(libraryName);

        customLibEditor.addRoot(VfsUtil.getUrlForLibraryRoot(classes), OrderRootType.CLASSES);
        customLibEditor.addRoot(VfsUtil.getUrlForLibraryRoot(sources), OrderRootType.SOURCES);

        ConfigLibraryUtil.addLibrary(customLibEditor, model);
    }

    @Override
    protected void tearDown() throws Exception {
        EdtTestUtil.runInEdtAndWait(new ThrowableRunnable<Throwable>() {
            @Override
            public void run() throws Throwable {
                ConfigLibraryUtil.removeLibrary(getModule(), CUSTOM_LIBRARY_NAME);
                ConfigLibraryUtil.removeLibrary(getModule(), KOTLIN_LIBRARY_NAME);
            }
        });

        super.tearDown();
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory());
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @Override
    protected void setUpModule() {
        super.setUpModule();

        IdeaTestUtil.setModuleLanguageLevel(myModule, LanguageLevel.JDK_1_6);

        if (!IS_TINY_APP_COMPILED) {
            String modulePath = getTestAppPath();

            CUSTOM_LIBRARY_JAR = MockLibraryUtil.compileLibraryToJar(CUSTOM_LIBRARY_SOURCES.getPath(), "debuggerCustomLibrary", false,
                                                                     false);

            String outputDir = getAppOutputPath();
            String sourcesDir = modulePath + File.separator + "src";

            MockLibraryUtil.compileKotlin(sourcesDir, new File(outputDir), CUSTOM_LIBRARY_JAR.getPath());

            List<String> options = Arrays.asList("-d", outputDir, "-classpath", ForTestCompileRuntime.runtimeJarForTests().getPath());
            try {
                KotlinTestUtils.compileJavaFiles(findJavaFiles(new File(sourcesDir)), options);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            IS_TINY_APP_COMPILED = true;
        }

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ModifiableRootModel model = ModuleRootManager.getInstance(myModule).getModifiableModel();
                configureLibrary(model, CUSTOM_LIBRARY_NAME, CUSTOM_LIBRARY_JAR, CUSTOM_LIBRARY_SOURCES);
                configureLibrary(model, KOTLIN_LIBRARY_NAME, ForTestCompileRuntime.runtimeJarForTests(), new File("libraries/stdlib/src"));
                model.commit();
            }
        });
    }

    private static List<File> findJavaFiles(@NotNull File directory) {
        List<File> result = new ArrayList<File>();
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

    private static class KotlinOutputChecker extends OutputChecker {

        public KotlinOutputChecker(@NotNull String appPath, @NotNull String outputPath) {
            super(appPath, outputPath);
        }

        @Override
        protected String replaceAdditionalInOutput(String str) {
            //noinspection ConstantConditions
            try {
                return super.replaceAdditionalInOutput(
                        str.replace(ForTestCompileRuntime.runtimeJarForTests().getCanonicalPath(), "!KOTLIN_RUNTIME!")
                           .replace(CUSTOM_LIBRARY_JAR.getCanonicalPath(), "!CUSTOM_LIBRARY!")
                );
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected JavaParameters createJavaParameters(String mainClass) {
        JavaParameters parameters = super.createJavaParameters(mainClass);
        parameters.getClassPath().add(ForTestCompileRuntime.runtimeJarForTests());
        parameters.getClassPath().add(CUSTOM_LIBRARY_JAR);
        return parameters;
    }

    @Override
    protected void createBreakpoints(final String className) {
        PsiClass[] psiClasses = ApplicationManager.getApplication().runReadAction(new Computable<PsiClass[]>() {
            @Override
            public PsiClass[] compute() {
                return JavaPsiFacade.getInstance(myProject).findClasses(className, GlobalSearchScope.allScope(myProject));
            }
        });

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
        try {
            super.checkTestOutput();
        }
        catch (ComparisonFailure e) {
            KotlinTestUtils.assertEqualsToFile(
                    new File(getTestAppPath() + File.separator + "outs" + File.separator + getTestName(true) + ".out"),
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
