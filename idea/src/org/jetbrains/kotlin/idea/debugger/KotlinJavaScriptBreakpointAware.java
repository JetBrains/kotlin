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

package org.jetbrains.kotlin.idea.debugger;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PairProcessor;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil;

public class KotlinJavaScriptBreakpointAware implements PairProcessor<VirtualFile, Project> {
    @Override
    public boolean process(VirtualFile file, Project project) {
        if (file.getFileType() == JetFileType.INSTANCE) {
            Module module = ModuleUtilCore.findModuleForFile(file, project);
            if (module != null) {
                return ProjectStructureUtil.isJsKotlinModule(module);
            }
        }
        return false;
    }
}
