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

/*
 * @author max
 */
package org.jetbrains.jet.cli.jvm.compiler;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CliJetFilesProvider extends JetFilesProvider {
    private final JetCoreEnvironment environment;
    private Function<JetFile,Collection<JetFile>> all_files = new Function<JetFile, Collection<JetFile>>() {
        @Override
        public Collection<JetFile> fun(JetFile file) {
            return environment.getSourceFiles();
        }

    };

    public CliJetFilesProvider(JetCoreEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Function<JetFile, Collection<JetFile>> sampleToAllFilesInModule() {
        return all_files;
    }

    @Override
    public List<JetFile> allInScope(GlobalSearchScope scope) {
        List<JetFile> answer = new ArrayList<JetFile>();
        for (JetFile file : environment.getSourceFiles()) {
            if (scope.contains(file.getVirtualFile())) {
                answer.add(file);
            }
        }
        return answer;
    }
}
