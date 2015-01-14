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

package org.jetbrains.kotlin.idea.k2jsrun;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.JavaScript;

import java.io.File;
import java.io.IOException;

public final class K2JSRunnerUtils {

    private K2JSRunnerUtils() {
    }

    @NotNull
    public static String constructPathToGeneratedFile(@NotNull Project project, @NotNull String outputDirPath) {
        return outputDirPath + "/" + project.getName() + JavaScript.DOT_EXTENSION;
    }

    public static void copyJSFileFromOutputToDestination(@NotNull Project project,
            @NotNull K2JSConfigurationSettings configurationSettings) {
        VirtualFile outputDir = getOutputDir(project);
        if (outputDir == null) {
            throw new RuntimeException("Cannot find output dir for project " + project.getName());
        }
        String pathToGeneratedJsFile = constructPathToGeneratedFile(project, outputDir.getPath());
        try {
            copyFileToDir(new File(pathToGeneratedJsFile), new File(configurationSettings.getGeneratedFilePath()));
        }
        catch (IOException e) {
            throw new RuntimeException("Output JavaScript file was not generated or missing.", e);
        }
    }

    @Nullable
    private static VirtualFile getOutputDir(@NotNull Project project) {
        Module module = getJsModule(project);
        return CompilerPaths.getModuleOutputDirectory(module, /*forTests = */ false);
    }

    @NotNull
    public static Module getJsModule(@NotNull Project project) {
        //TODO Should not be there, we should know what module we are in
        Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length != 1) {
            throw new UnsupportedOperationException("Kotlin to JavaScript translator temporarily does not support multiple modules.");
        }
        return modules[0];
    }

    public static void openBrowser(@NotNull K2JSConfigurationSettings configurationSettings) {
        if (!configurationSettings.isShouldOpenInBrowserAfterTranslation()) {
            return;
        }
        String filePath = configurationSettings.getPageToOpenFilePath();
        String url = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, filePath);

        WebBrowser browser = WebBrowserManager.getInstance().findBrowserById(configurationSettings.getBrowserFamily().getName());
        BrowserLauncher.getInstance().browse(url, browser);
    }

    @NotNull
    public static K2JSConfigurationSettings getSettings(@NotNull ExecutionEnvironment env) {
        RunProfile profile = env.getRunProfile();
        assert profile instanceof K2JSRunConfiguration;
        return ((K2JSRunConfiguration) profile).settings();
    }

    //TODO: this method does not really belong here, but dunno where it should be
    public static void copyFileToDir(@NotNull File file, @NotNull File dir) throws IOException {
        FileUtil.copy(file, new File(dir, file.getName()));
    }
}
