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

package org.jetbrains.kotlin.android;

import com.android.SdkConstants;
import com.android.tools.idea.rendering.RenderSecurityManager;
import com.android.tools.idea.res.PsiProjectListener;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.AndroidRootUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@SuppressWarnings({"JUnitTestCaseWithNonTrivialConstructors"})
public abstract class KotlinAndroidTestCase extends KotlinAndroidTestCaseBase {
    protected Module myModule;
    protected List<Module> myAdditionalModules;

    private final boolean myCreateManifest;
    protected AndroidFacet myFacet;

    private boolean kotlinInternalModeOriginalValue;

    public KotlinAndroidTestCase(boolean createManifest) {
        this.myCreateManifest = createManifest;
    }

    public KotlinAndroidTestCase() {
        this(true);
    }

    protected File[] getResourceDirs(String path) {
        return new File(path).listFiles(new FileFilter() {
            @Override
            public boolean accept(@NotNull File file) {
                return file.getName().startsWith("res") && file.isDirectory();
            }
        });
    }

    @Override
    protected void setUp() throws Exception {
        // sdk path workaround, set real android sdk path and platform for android plugin to work
        System.setProperty(KotlinAndroidTestCaseBase.SDK_PATH_PROPERTY, PathManager.getHomePath() + "/../dependencies/androidSDK");
        System.setProperty(KotlinAndroidTestCaseBase.PLATFORM_DIR_PROPERTY, "android-23");

        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory());

        super.setUp();

        // this will throw an exception if we don't have a full Android SDK, so we need to do this first thing before any other setup
        String sdkPath = getTestSdkPath();

        final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder =
                IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName());
        myFixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(projectBuilder.getFixture());
        final JavaModuleFixtureBuilder moduleFixtureBuilder = projectBuilder.addModule(JavaModuleFixtureBuilder.class);
        final String dirPath = myFixture.getTempDirPath() + getContentRootPath();
        final File dir = new File(dirPath);

        if (!dir.exists()) {
            assertTrue(dir.mkdirs());
        }
        tuneModule(moduleFixtureBuilder, dirPath);

        final List<MyAdditionalModuleData> modules = new ArrayList<MyAdditionalModuleData>();
        configureAdditionalModules(projectBuilder, modules);

        myFixture.setUp();
        myFixture.setTestDataPath(getTestDataPath());
        myModule = moduleFixtureBuilder.getFixture().getModule();

        // Must be done before addAndroidFacet, and must always be done, even if !myCreateManifest.
        // We will delete it at the end of setUp; this is needed when unit tests want to rewrite
        // the manifest on their own.
        createManifest();

        androidSdk = createAndroidSdk(getTestSdkPath(), getPlatformDir());
        myFacet = addAndroidFacet(myModule, sdkPath, getPlatformDir(), isToAddSdk());
        for (File resDir : getResourceDirs(dirPath)) {
            if (resDir.exists()) {
                myFixture.copyDirectoryToProject(resDir.getName(), resDir.getName());
            } else {
                Logger.getInstance(this.getClass()).info("No res directory found in test");
            }
        }
        myAdditionalModules = new ArrayList<Module>();

        for (MyAdditionalModuleData data : modules) {
            final Module additionalModule = data.myModuleFixtureBuilder.getFixture().getModule();
            myAdditionalModules.add(additionalModule);
            final AndroidFacet facet = addAndroidFacet(additionalModule, sdkPath, getPlatformDir());
            facet.setProjectType(data.myLibrary ? 1 : 0);
            final String rootPath = getContentRootPath(data.myDirName);
            myFixture.copyDirectoryToProject("res", rootPath + "/res");
            myFixture.copyFileToProject(SdkConstants.FN_ANDROID_MANIFEST_XML,
                                        rootPath + '/' + SdkConstants.FN_ANDROID_MANIFEST_XML);
            ModuleRootModificationUtil.addDependency(myModule, additionalModule);
        }

        if (!myCreateManifest) {
            deleteManifest();
        }

        if (RenderSecurityManager.RESTRICT_READS) {
            // Unit test class loader includes disk directories which security manager does not allow access to
            RenderSecurityManager.sEnabled = false;
        }

        ((StartupManagerImpl) StartupManager.getInstance(getProject())).runPostStartupActivities();

        kotlinInternalModeOriginalValue = KotlinInternalMode.Instance.getEnabled();
        KotlinInternalMode.Instance.setEnabled(true);
    }

    protected boolean isToAddSdk() {
        return true;
    }

    protected String getContentRootPath() {
        return "";
    }

    protected void configureAdditionalModules(@NotNull TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder,
            @NotNull List<MyAdditionalModuleData> modules) {
    }

    protected static String getContentRootPath(@NotNull String moduleName) {
        return "/additionalModules/" + moduleName;
    }

    public static void tuneModule(JavaModuleFixtureBuilder moduleBuilder, String moduleDirPath) {
        moduleBuilder.addContentRoot(moduleDirPath);

        //noinspection ResultOfMethodCallIgnored
        new File(moduleDirPath + "/src/").mkdir();
        moduleBuilder.addSourceRoot("src");

        //noinspection ResultOfMethodCallIgnored
        new File(moduleDirPath + "/gen/").mkdir();
        moduleBuilder.addSourceRoot("gen");
    }

    protected void createManifest() throws IOException {
        myFixture.copyFileToProject("plugins/android-extensions/android-extensions-idea/testData/android/AndroidManifest.xml",
                                    SdkConstants.FN_ANDROID_MANIFEST_XML);
    }

    protected void deleteManifest() throws IOException {
        deleteManifest(myModule);
    }

    protected void deleteManifest(final Module module) throws IOException {
        final AndroidFacet facet = AndroidFacet.getInstance(module);
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

    @Override
    public void tearDown() throws Exception {
        KotlinInternalMode.Instance.setEnabled(kotlinInternalModeOriginalValue);

        Field listenersField = PsiProjectListener.class.getDeclaredField("myListeners");
        listenersField.setAccessible(true);
        Map listeners = (Map)listenersField.get(PsiProjectListener.getInstance(getProject()));
        listeners.clear();

        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory());

        super.tearDown();

        myModule = null;
        myAdditionalModules = null;
        myFixture.tearDown();
        myFixture = null;
        myFacet = null;

        if (RenderSecurityManager.RESTRICT_READS) {
            RenderSecurityManager.sEnabled = true;
        }
    }

    public AndroidFacet addAndroidFacet(Module module, String sdkPath, String platformDir) {
        return addAndroidFacet(module, sdkPath, platformDir, true);
    }

    public AndroidFacet addAndroidFacet(Module module, String sdkPath, String platformDir, boolean addSdk) {
        FacetManager facetManager = FacetManager.getInstance(module);
        AndroidFacet facet = facetManager.createFacet(AndroidFacet.getFacetType(), "Android", null);

        if (addSdk) {
            addAndroidSdk(module, sdkPath, platformDir);
        }
        final ModifiableFacetModel facetModel = facetManager.createModifiableModel();
        facetModel.addFacet(facet);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                facetModel.commit();
            }
        });
        return facet;
    }

    protected static class MyAdditionalModuleData {
        final JavaModuleFixtureBuilder myModuleFixtureBuilder;
        final String myDirName;
        final boolean myLibrary;

        private MyAdditionalModuleData(@NotNull JavaModuleFixtureBuilder moduleFixtureBuilder,
                @NotNull String dirName,
                boolean library) {
            myModuleFixtureBuilder = moduleFixtureBuilder;
            myDirName = dirName;
            myLibrary = library;
        }
    }
}
