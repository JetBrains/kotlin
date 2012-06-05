/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;

/**
 * @author Pavel Talanov
 */
public final class AnalyzerFacadeProvider {

    private final static Logger LOG = Logger.getInstance(AnalyzerFacade.class);

    private AnalyzerFacadeProvider() {
    }

    @NotNull
    public static AnalyzerFacade getAnalyzerFacadeForFile(@NotNull JetFile file) {
        VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
        if (virtualFile == null) {
            logErrorIfNotTests("No virtual file for " + file.getName() + " with text:\n" + file.getText());
            return getDefaultAnalyzerFacade();
        }
        Module moduleForFile = ProjectFileIndex.SERVICE.getInstance(file.getProject()).getModuleForFile(virtualFile);
        if (moduleForFile == null) {
            logErrorIfNotTests("File " + virtualFile.getPath() + " is not under any module. Cannot determine which facade to use.");
            return getDefaultAnalyzerFacade();
        }
        return getAnalyzerFacadeForModule(moduleForFile);
    }

    private static void logErrorIfNotTests(@NotNull String message) {
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            LOG.error(message);
        }
    }

    @NotNull
    private static AnalyzerFacade getDefaultAnalyzerFacade() {
        LOG.info("Using default analyzer facade");
        return AnalyzerFacadeForJVM.INSTANCE;
    }

    @NotNull
    private static AnalyzerFacade getAnalyzerFacadeForModule(@NotNull Module module) {
        if (JsModuleDetector.isJsModule(module)) {
            return JSAnalyzerFacadeForIDEA.INSTANCE;
        }
        return AnalyzerFacadeForJVM.INSTANCE;
    }
}
