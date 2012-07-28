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

package org.jetbrains.jet.plugin.compiler;

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.Collections;

/**
 * @author yole
 */
public class JetCompilerManager implements ProjectComponent {
    public JetCompilerManager(CompilerManager manager) {
        manager.addTranslatingCompiler(new JetCompiler(),
                                       Collections.<FileType>singleton(JetFileType.INSTANCE),
                                       Collections.singleton(StdFileTypes.CLASS));
        manager.addTranslatingCompiler(new K2JSCompiler(),
                                       Collections.<FileType>singleton(JetFileType.INSTANCE),
                                       Collections.<FileType>singleton(StdFileTypes.JS));
        manager.addCompilableFileType(JetFileType.INSTANCE);
    }

    @Override
    public void projectOpened() {
    }

    @Override
    public void projectClosed() {
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return JetCompilerManager.class.getCanonicalName();
    }
}
