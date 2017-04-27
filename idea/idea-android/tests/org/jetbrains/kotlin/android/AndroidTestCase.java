/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jetbrains.kotlin.android;

import com.android.SdkConstants;
import com.android.tools.idea.rendering.RenderSecurityManager;
import com.android.tools.idea.startup.AndroidCodeStyleSettingsModifier;
import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.GlobalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ex.GlobalInspectionToolWrapper;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.codeStyle.CodeStyleSchemes;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.InspectionTestUtil;
import com.intellij.testFramework.ThreadTracker;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import com.intellij.testFramework.fixtures.impl.GlobalInspectionContextForTests;
import com.intellij.util.ArrayUtil;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.AndroidRootUtil;
import org.jetbrains.android.formatter.AndroidXmlCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.picocontainer.MutablePicoContainer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *  Copied from AS 2.3 sources
 */
@SuppressWarnings({"JUnitTestCaseWithNonTrivialConstructors"})
public abstract class AndroidTestCase extends AndroidTestBase {
    protected Module myModule;
    protected List<Module> myAdditionalModules;

    protected AndroidFacet myFacet;
    protected CodeStyleSettings mySettings;

    private List<String> myAllowedRoots = new ArrayList<>();
    private boolean myUseCustomSettings;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory());

        TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName());
        myFixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(projectBuilder.getFixture());
        JavaModuleFixtureBuilder moduleFixtureBuilder = projectBuilder.addModule(JavaModuleFixtureBuilder.class);
        File moduleRoot = new File(myFixture.getTempDirPath());

        if (!moduleRoot.exists()) {
            assertTrue(moduleRoot.mkdirs());
        }
        initializeModuleFixtureBuilderWithSrcAndGen(moduleFixtureBuilder, moduleRoot.toString());

        ArrayList<MyAdditionalModuleData> modules = new ArrayList<>();
        configureAdditionalModules(projectBuilder, modules);

        myFixture.setUp();
        myFixture.setTestDataPath(getTestDataPath());
        myModule = moduleFixtureBuilder.getFixture().getModule();

        // Must be done before addAndroidFacet, and must always be done, even if a test provides
        // its own custom manifest file. However, in that case, we will delete it shortly below.
        createManifest();

        myFacet = addAndroidFacet(myModule);

        LanguageLevel languageLevel = getLanguageLevel();
        if (languageLevel != null) {
            LanguageLevelProjectExtension extension = LanguageLevelProjectExtension.getInstance(myModule.getProject());
            if (extension != null) {
                extension.setLanguageLevel(languageLevel);
            }
        }

        // TODO: myFixture.copyDirectoryToProject(getResDir(), "res");

        myAdditionalModules = new ArrayList<>();
        for (MyAdditionalModuleData data : modules) {
            Module additionalModule = data.myModuleFixtureBuilder.getFixture().getModule();
            myAdditionalModules.add(additionalModule);
            AndroidFacet facet = addAndroidFacet(additionalModule);
            facet.setProjectType(data.myProjectType);
            String rootPath = getAdditionalModulePath(data.myDirName);
            myFixture.copyDirectoryToProject(getResDir(), rootPath + "/res");
            myFixture.copyFileToProject(SdkConstants.FN_ANDROID_MANIFEST_XML, rootPath + '/' + SdkConstants.FN_ANDROID_MANIFEST_XML);
            if (data.myIsMainModuleDependency) {
                ModuleRootModificationUtil.addDependency(myModule, additionalModule);
            }
        }

        if (providesCustomManifest()) {
            deleteManifest();
        }

        if (RenderSecurityManager.RESTRICT_READS) {
            // Unit test class loader includes disk directories which security manager does not allow access to
            RenderSecurityManager.sEnabled = false;
        }

        ArrayList<String> allowedRoots = new ArrayList<>();
        collectAllowedRoots(allowedRoots);
        // TODO: registerAllowedRoots(allowedRoots, myTestRootDisposable);
        mySettings = CodeStyleSettingsManager.getSettings(getProject()).clone();
        AndroidCodeStyleSettingsModifier.modify(mySettings);
        CodeStyleSettingsManager.getInstance(getProject()).setTemporarySettings(mySettings);
        myUseCustomSettings = getAndroidCodeStyleSettings().USE_CUSTOM_SETTINGS;
        getAndroidCodeStyleSettings().USE_CUSTOM_SETTINGS = true;

        // Layoutlib rendering thread will be shutdown when the app is closed so do not report it as a leak
        ThreadTracker.longRunningThreadCreated(ApplicationManager.getApplication(), "Layoutlib");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            CodeStyleSettingsManager.getInstance(getProject()).dropTemporarySettings();
            myModule = null;
            myAdditionalModules = null;
            myFixture.tearDown();
            myFixture = null;
            myFacet = null;
            getAndroidCodeStyleSettings().USE_CUSTOM_SETTINGS = myUseCustomSettings;
            if (RenderSecurityManager.RESTRICT_READS) {
                RenderSecurityManager.sEnabled = true;
            }
        }
        finally {
            super.tearDown();
            VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory());
        }
    }

    private static void initializeModuleFixtureBuilderWithSrcAndGen(JavaModuleFixtureBuilder moduleFixtureBuilder, String moduleRoot) {
        moduleFixtureBuilder.addContentRoot(moduleRoot);

        //noinspection ResultOfMethodCallIgnored
        new File(moduleRoot + "/src/").mkdir();
        moduleFixtureBuilder.addSourceRoot("src");

        //noinspection ResultOfMethodCallIgnored
        new File(moduleRoot + "/gen/").mkdir();
        moduleFixtureBuilder.addSourceRoot("gen");
    }

    /**
     * Returns the path that any additional modules registered by
     * {@link #configureAdditionalModules(TestFixtureBuilder, List)} or
     * {@link #addModuleWithAndroidFacet(TestFixtureBuilder, List, String, int, boolean)} are
     * installed into.
     */
    protected static String getAdditionalModulePath(@NotNull String moduleName) {
        return "/additionalModules/" + moduleName;
    }

    /**
     * Indicates whether this class provides its own {@code AndroidManifest.xml} for its tests. If
     * {@code true}, then {@link #setUp()} calls {@link #deleteManifest()} before finishing.
     */
    protected boolean providesCustomManifest() {
        return false;
    }

    /**
     * Get the "res" directory for this SDK. Children classes can override this if they need to
     * provide a custom "res" location for tests.
     */
    protected String getResDir() {
        return "res";
    }

    /**
     * Defines the project level to set for the test project, or null to get the default language
     * level associated with the test project.
     */
    @Nullable
    protected LanguageLevel getLanguageLevel() {
        return null;
    }

    protected static AndroidXmlCodeStyleSettings getAndroidCodeStyleSettings() {
        return AndroidXmlCodeStyleSettings.getInstance(CodeStyleSchemes.getInstance().getDefaultScheme().getCodeStyleSettings());
    }

    /**
     * Hook point for child test classes to register directories that can be safely accessed by all
     * of its tests.
     *
     * @see {@link VfsRootAccess}
     */
    protected void collectAllowedRoots(List<String> roots) throws IOException {
    }

    private void registerAllowedRoots(List<String> roots, @NotNull Disposable disposable) {
        List<String> newRoots = new ArrayList<>(roots);
        newRoots.removeAll(myAllowedRoots);

        String[] newRootsArray = ArrayUtil.toStringArray(newRoots);
        VfsRootAccess.allowRootAccess(newRootsArray);
        myAllowedRoots.addAll(newRoots);

        Disposer.register(disposable, () -> {
            VfsRootAccess.disallowRootAccess(newRootsArray);
            myAllowedRoots.removeAll(newRoots);
        });
    }

    public static AndroidFacet addAndroidFacet(Module module) {
        return addAndroidFacet(module, true);
    }

    private static AndroidFacet addAndroidFacet(Module module, boolean attachSdk) {
        FacetManager facetManager = FacetManager.getInstance(module);
        AndroidFacet facet = facetManager.createFacet(AndroidFacet.getFacetType(), "Android", null);

        if (attachSdk) {
            addLatestAndroidSdk(module);
        }
        ModifiableFacetModel facetModel = facetManager.createModifiableModel();
        facetModel.addFacet(facet);
        ApplicationManager.getApplication().runWriteAction(facetModel::commit);
        return facet;
    }

    protected void configureAdditionalModules(
            @NotNull TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder, @NotNull List<MyAdditionalModuleData> modules) {
    }

    protected final void addModuleWithAndroidFacet(
            @NotNull TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder,
            @NotNull List<MyAdditionalModuleData> modules,
            @NotNull String dirName,
            int projectType) {
        // By default, created module is declared as a main module's dependency
        addModuleWithAndroidFacet(projectBuilder, modules, dirName, projectType, true);
    }

    protected final void addModuleWithAndroidFacet(
            @NotNull TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder,
            @NotNull List<MyAdditionalModuleData> modules,
            @NotNull String dirName,
            int projectType,
            boolean isMainModuleDependency) {
        JavaModuleFixtureBuilder moduleFixtureBuilder = projectBuilder.addModule(JavaModuleFixtureBuilder.class);
        String moduleDirPath = myFixture.getTempDirPath() + getAdditionalModulePath(dirName);
        //noinspection ResultOfMethodCallIgnored
        new File(moduleDirPath).mkdirs();
        initializeModuleFixtureBuilderWithSrcAndGen(moduleFixtureBuilder, moduleDirPath);
        modules.add(new MyAdditionalModuleData(moduleFixtureBuilder, dirName, projectType, isMainModuleDependency));
    }

    protected void createManifest() throws IOException {
        myFixture.copyFileToProject(SdkConstants.FN_ANDROID_MANIFEST_XML, SdkConstants.FN_ANDROID_MANIFEST_XML);
    }

    protected final void createProjectProperties() throws IOException {
        myFixture.copyFileToProject(SdkConstants.FN_PROJECT_PROPERTIES, SdkConstants.FN_PROJECT_PROPERTIES);
    }

    protected final void deleteManifest() throws IOException {
        deleteManifest(myModule);
    }

    protected final void deleteManifest(final Module module) throws IOException {
        AndroidFacet facet = AndroidFacet.getInstance(module);
        assertNotNull(facet);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                String manifestRelativePath = facet.getProperties().MANIFEST_FILE_RELATIVE_PATH;
                VirtualFile manifest = AndroidRootUtil.getFileByRelativeModulePath(module, manifestRelativePath, true);
                if (manifest != null) {
                    try {
                        manifest.delete(this);
                    }
                    catch (IOException e) {
                        fail("Could not delete default manifest");
                    }
                }
            }
        });
    }

    protected final void doGlobalInspectionTest(
            @NotNull GlobalInspectionTool inspection, @NotNull String globalTestDir, @NotNull AnalysisScope scope) {
        doGlobalInspectionTest(new GlobalInspectionToolWrapper(inspection), globalTestDir, scope);
    }

    /**
     * Given an inspection and a path to a directory that contains an "expected.xml" file, run the
     * inspection on the current test project and verify that its output matches that of the
     * expected file.
     */
    protected final void doGlobalInspectionTest(
            @NotNull GlobalInspectionToolWrapper wrapper, @NotNull String globalTestDir, @NotNull AnalysisScope scope) {
        myFixture.enableInspections(wrapper.getTool());

        scope.invalidate();

        InspectionManagerEx inspectionManager = (InspectionManagerEx)InspectionManager.getInstance(getProject());
        GlobalInspectionContextForTests globalContext =
                CodeInsightTestFixtureImpl.createGlobalContextForTool(scope, getProject(), inspectionManager, wrapper);

        InspectionTestUtil.runTool(wrapper, scope, globalContext);
        InspectionTestUtil.compareToolResults(globalContext, wrapper, false, getTestDataPath() + globalTestDir);
    }

    protected static class MyAdditionalModuleData {
        final JavaModuleFixtureBuilder myModuleFixtureBuilder;
        final String myDirName;
        final int myProjectType;
        final boolean myIsMainModuleDependency;

        private MyAdditionalModuleData(
                @NotNull JavaModuleFixtureBuilder moduleFixtureBuilder, @NotNull String dirName, int projectType, boolean isMainModuleDependency) {
            myModuleFixtureBuilder = moduleFixtureBuilder;
            myDirName = dirName;
            myProjectType = projectType;
            myIsMainModuleDependency = isMainModuleDependency;
        }
    }

    @NotNull
    protected <T> T registerApplicationComponent(@NotNull Class<T> key, @NotNull T instance) throws Exception {
        MutablePicoContainer picoContainer = (MutablePicoContainer)ApplicationManager.getApplication().getPicoContainer();
        @SuppressWarnings("unchecked")
        T old = (T)picoContainer.getComponentInstance(key.getName());
        picoContainer.unregisterComponent(key.getName());
        picoContainer.registerComponentInstance(key.getName(), instance);
        return old;
    }

    @NotNull
    protected <T> T registerProjectComponent(@NotNull Class<T> key, @NotNull T instance) {
        MutablePicoContainer picoContainer = (MutablePicoContainer)getProject().getPicoContainer();
        @SuppressWarnings("unchecked")
        T old = (T)picoContainer.getComponentInstance(key.getName());
        picoContainer.unregisterComponent(key.getName());
        picoContainer.registerComponentInstance(key.getName(), instance);
        return old;
    }
}
