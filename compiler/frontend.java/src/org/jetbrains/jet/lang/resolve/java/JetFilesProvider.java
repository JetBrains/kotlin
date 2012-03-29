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
package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.Collection;
import java.util.List;

public abstract class JetFilesProvider {
    public static JetFilesProvider getInstance(Project project) {
        return ServiceManager.getService(project, JetFilesProvider.class);
    }

    public abstract Function<JetFile, Collection<JetFile>> sampleToAllFilesInModule();
    public abstract List<JetFile> allInScope(GlobalSearchScope scope);
}
