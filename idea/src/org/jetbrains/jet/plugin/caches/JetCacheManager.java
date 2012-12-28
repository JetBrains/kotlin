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

package org.jetbrains.jet.plugin.caches;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.project.JsModuleDetector;

public class JetCacheManager implements ProjectComponent {
    private Project myProject;
    private JetShortNamesCache myCache;

    public static JetCacheManager getInstance(Project project) {
        return project.getComponent(JetCacheManager.class);
    }

    public JetCacheManager(Project project) {
        myProject = project;
    }

    @Override
    public void projectOpened() {

    }

    @Override
    public void projectClosed() {

    }

    @Override
    @NotNull
    public String getComponentName() {
        return "Kotlin caches manager";
    }

    @Override
    public void initComponent() {
        myCache = new JetShortNamesCache(myProject);
    }

    @Override
    public void disposeComponent() {

    }

    public JetShortNamesCache getNamesCache() {
        return myCache;
    }

    public PsiShortNamesCache getShortNamesCache(@NotNull JetFile jetFile) {
        if (JsModuleDetector.isJsModule(jetFile)) {
            return myCache;
        }

        return PsiShortNamesCache.getInstance(myProject);
    }
}
