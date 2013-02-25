/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.plugin.framework.FrameworkDetector;

public final class AnalyzerFacadeProvider {

    private final static Logger LOG = Logger.getInstance(AnalyzerFacade.class);

    private AnalyzerFacadeProvider() {
    }

    @NotNull
    public static AnalyzerFacade getAnalyzerFacadeForFile(@NotNull JetFile file) {
        VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
        if (virtualFile == null) {
            return getDefaultAnalyzerFacade();
        }
        Module moduleForFile = ProjectFileIndex.SERVICE.getInstance(file.getProject()).getModuleForFile(virtualFile);
        if (moduleForFile == null) {
            return getDefaultAnalyzerFacade();
        }
        return getAnalyzerFacadeForModule(moduleForFile);
    }

    @NotNull
    private static AnalyzerFacade getDefaultAnalyzerFacade() {
        //TODO: should deal with situations when we can't determine whether to use java or js backend more carefully
        LOG.info("Using default analyzer facade");
        return AnalyzerFacadeForJVM.INSTANCE;
    }

    @NotNull
    private static AnalyzerFacade getAnalyzerFacadeForModule(@NotNull Module module) {
        if (FrameworkDetector.isJsModule(module)) {
            return JSAnalyzerFacadeForIDEA.INSTANCE;
        }
        return AnalyzerFacadeForJVM.INSTANCE;
    }
}
