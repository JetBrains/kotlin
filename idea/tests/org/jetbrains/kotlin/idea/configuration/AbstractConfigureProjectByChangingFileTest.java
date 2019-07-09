/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaModule;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightCodeInsightTestCase;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.core.script.ScriptsCompilationConfigurationUpdaterKt;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.plugins.groovy.GroovyFileType;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public abstract class AbstractConfigureProjectByChangingFileTest<C extends KotlinProjectConfigurator> extends LightCodeInsightTestCase {
    private static final String DEFAULT_VERSION = "default_version";

    private PsiFile moduleInfoFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ApplicationManager.getApplication().runWriteAction(
                () -> FileTypeManager.getInstance().associateExtension(GroovyFileType.GROOVY_FILE_TYPE, "gradle")
        );
        ScriptsCompilationConfigurationUpdaterKt.setScriptDependenciesUpdaterDisabled(ApplicationManager.getApplication(), true);
    }

    @Override
    protected void tearDown() throws Exception {
        ScriptsCompilationConfigurationUpdaterKt.setScriptDependenciesUpdaterDisabled(ApplicationManager.getApplication(), false);
        moduleInfoFile = null;
        super.tearDown();
    }

    protected void doTest(@NotNull String beforeFile, @NotNull String afterFile, @NotNull C configurator) throws Exception {
        configureByFile(beforeFile);

        prepareModuleInfoFile(beforeFile);

        String versionFromFile = InTextDirectivesUtils.findStringWithPrefixes(getFile().getText(), "// VERSION:");
        String version = versionFromFile != null ? versionFromFile : DEFAULT_VERSION;

        NotificationMessageCollector collector = NotificationMessageCollectorKt.createConfigureKotlinNotificationCollector(getProject());

        runConfigurator(getModule(), getFile(), configurator, version, collector);

        collector.showNotification();

        KotlinTestUtils.assertEqualsToFile(new File(afterFile), getFile().getText().replace(version, "$VERSION$"));

        checkModuleInfoFile(beforeFile);
    }

    private void prepareModuleInfoFile(@NotNull String beforeFile) throws IOException {
        File file = new File(beforeFile);
        String parent = file.getParent();
        File moduleInfo = new File(parent, PsiJavaModule.MODULE_INFO_FILE);
        moduleInfoFile = null;
        if (moduleInfo.exists()) {
            String fileText = FileUtilRt.loadFile(moduleInfo, CharsetToolkit.UTF8, true);
            createAndSaveFile(PsiJavaModule.MODULE_INFO_FILE, fileText);

            PsiFile[] moduleInfoFiles =
                    FilenameIndex.getFilesByName(getProject(), PsiJavaModule.MODULE_INFO_FILE, GlobalSearchScope.allScope(getProject()));
            assertTrue(PsiJavaModule.MODULE_INFO_FILE + " should be present in index", moduleInfoFiles.length == 1);
            moduleInfoFile = moduleInfoFiles[0];
        }
    }

    private void checkModuleInfoFile(@NotNull String beforeFile) {
        File file = new File(beforeFile);
        String parent = file.getParent();

        if (moduleInfoFile != null) {
            String afterFileName = PsiJavaModule.MODULE_INFO_FILE.replace(".java", "_after.java");
            commitAllDocuments();
            KotlinTestUtils.assertEqualsToFile(new File(parent, afterFileName), moduleInfoFile.getText());
        }
    }

    protected abstract void runConfigurator(
            Module module, @NotNull PsiFile file,
            @NotNull C configurator,
            @NotNull String version,
            @NotNull NotificationMessageCollector collector
    );

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new SimpleLightProjectDescriptor(getModuleType(), getProjectJDK());
    }

    private static class SimpleLightProjectDescriptor extends LightProjectDescriptor {
        @NotNull private final ModuleType myModuleType;
        @Nullable private final Sdk mySdk;

        SimpleLightProjectDescriptor(@NotNull ModuleType moduleType, @Nullable Sdk sdk) {
            myModuleType = moduleType;
            mySdk = sdk;
        }

        @NotNull
        @Override
        public ModuleType getModuleType() {
            return myModuleType;
        }

        @Nullable
        @Override
        public Sdk getSdk() {
            return mySdk;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SimpleLightProjectDescriptor that = (SimpleLightProjectDescriptor)o;

            if (!myModuleType.equals(that.myModuleType)) return false;
            return areJdksEqual(that.getSdk());
        }

        @Override
        public int hashCode() {
            return myModuleType.hashCode();
        }

        private boolean areJdksEqual(Sdk newSdk) {
            if (mySdk == null || newSdk == null) return mySdk == newSdk;

            if (!Objects.equals(mySdk.getVersionString(), newSdk.getVersionString())) {
                return false;
            }

            String[] myUrls = mySdk.getRootProvider().getUrls(OrderRootType.CLASSES);
            String[] newUrls = newSdk.getRootProvider().getUrls(OrderRootType.CLASSES);
            return ContainerUtil.newHashSet(myUrls).equals(ContainerUtil.newHashSet(newUrls));
        }
    }
}
