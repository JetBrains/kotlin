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

package org.jetbrains.jet.plugin.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.codegen.binding.PsiCodegenPredictor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;

import java.util.Collection;

import static org.jetbrains.jet.plugin.stubindex.PackageIndexUtil.findFilesWithExactPackage;

public class DebuggerUtils {
    private DebuggerUtils() {
    }

    @Nullable
    public static JetFile findSourceFileForClass(
            @NotNull Project project,
            @NotNull GlobalSearchScope searchScope,
            @NotNull JvmClassName className,
            @NotNull final String fileName
    ) {

        FqName packageFqName = getPackageFqNameForClass(className);

        Collection<JetFile> filesInPackage = findFilesWithExactPackage(packageFqName, searchScope, project);
        Collection<JetFile> filesWithExactName = Collections2.filter(filesInPackage, new Predicate<JetFile>() {
            @Override
            public boolean apply(@Nullable JetFile file) {
                return file != null && file.getName().equals(fileName);
            }
        });

        if (filesWithExactName.isEmpty()) return null;

        if (filesWithExactName.size() == 1) {
            return filesWithExactName.iterator().next();
        }

        JetFile file = PsiCodegenPredictor.getFileForPackagePartName(filesWithExactName, className);
        if (file != null) {
            return file;
        }

        // In the rare case that there's more than one file with this name in this package,
        // we may actually need to analyze the project in order to find a file which produces this class
        // TODO: this code is not entirely correct, because it takes a session for only one file
        AnalyzeExhaust analyzeExhaust = ResolvePackage.getAnalysisResultsForElements(filesWithExactName);

        return PsiCodegenPredictor.getFileForCodegenNamedClass(analyzeExhaust.getModuleDescriptor(), analyzeExhaust.getBindingContext(),
                                                               filesWithExactName, className.getInternalName());
    }

    @NotNull
    private static FqName getPackageFqNameForClass(@NotNull JvmClassName className) {
        String internalName = className.getInternalName();
        int lastSlash = internalName.lastIndexOf('/');
        return lastSlash == -1 ? FqName.ROOT : new FqName(internalName.substring(0, lastSlash).replace('/', '.'));
    }
}
