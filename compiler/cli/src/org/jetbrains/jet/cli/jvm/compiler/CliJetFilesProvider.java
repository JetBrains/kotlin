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

package org.jetbrains.jet.cli.jvm.compiler;

import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;

import java.util.ArrayList;
import java.util.List;

public class CliJetFilesProvider extends JetFilesProvider {
    private final JetCoreEnvironment environment;

    public CliJetFilesProvider(JetCoreEnvironment environment) {
        this.environment = environment;
    }

    @NotNull
    @Override
    public List<JetFile> allInScope(@NotNull GlobalSearchScope scope) {
        List<JetFile> answer = new ArrayList<JetFile>();
        for (JetFile file : environment.getSourceFiles()) {
            if (scope.contains(file.getVirtualFile())) {
                answer.add(file);
            }
        }
        return answer;
    }

    @Override
    public boolean isFileInScope(@NotNull JetFile file, @NotNull GlobalSearchScope scope) {
        return scope.contains(file.getVirtualFile()) && environment.getSourceFiles().contains(file);
    }
}
