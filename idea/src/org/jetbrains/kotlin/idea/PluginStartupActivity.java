/*
 * Copyright 2010-2020 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.searches.IndexPatternSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.reporter.KotlinReportSubmitter;
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinTodoSearcher;
import org.jetbrains.kotlin.utils.PathUtil;

import static org.jetbrains.kotlin.idea.TestResourceBundleKt.registerAdditionalResourceBundleInTests;

public class PluginStartupActivity implements StartupActivity.DumbAware {
    private static final Logger LOG = Logger.getInstance(PluginStartupActivity.class);

    private static final String KOTLIN_BUNDLED = "KOTLIN_BUNDLED";

    @Override
    public void runActivity(@NotNull Project project) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            registerAdditionalResourceBundleInTests();
        }

        registerPathVariable();

        try {
            // API added in 15.0.2
            UpdateChecker.INSTANCE.getExcludedFromUpdateCheckPlugins().add("org.jetbrains.kotlin");
        }
        catch (Throwable throwable) {
            LOG.debug("Excluding Kotlin plugin updates using old API", throwable);
            UpdateChecker.getDisabledToUpdatePlugins().add("org.jetbrains.kotlin");
        }
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent e) {
                VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(e.getDocument());
                if (virtualFile != null && virtualFile.getFileType() == KotlinFileType.INSTANCE) {
                    KotlinPluginUpdater.Companion.getInstance().kotlinFileEdited(virtualFile);
                }
            }
        }, project);

        ServiceManager.getService(IndexPatternSearch.class).registerExecutor(new KotlinTodoSearcher());

        KotlinPluginCompatibilityVerifier.checkCompatibility();

        KotlinReportSubmitter.Companion.setupReportingFromRelease();

        //todo[Sedunov]: wait for fix in platform to avoid misunderstood from Java newbies (also ConfigureKotlinInTempDirTest)
        //KotlinSdkType.Companion.setUpIfNeeded();
    }

    private static void registerPathVariable() {
        PathMacros macros = PathMacros.getInstance();
        macros.setMacro(KOTLIN_BUNDLED, PathUtil.getKotlinPathsForIdeaPlugin().getHomePath().getPath());
    }

}
