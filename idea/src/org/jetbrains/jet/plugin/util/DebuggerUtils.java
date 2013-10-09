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
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.codegen.binding.PsiCodegenPredictor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

import java.util.Collection;

public class DebuggerUtils {
    private DebuggerUtils() {
    }

    @Nullable
    public static JetFile findSourceFileForClass(
            @NotNull GlobalSearchScope searchScope,
            @NotNull JvmClassName className,
            @NotNull final String fileName
    ) {
        JetFilesProvider filesProvider = JetFilesProvider.getInstance(searchScope.getProject());
        Collection<JetFile> filesInScope = filesProvider.allInScope(searchScope);

        final FqName packageFqName = getPackageFqNameForClass(className);

        // Only consider files with the file name from the stack trace and in the given package
        Collection<JetFile> files = Collections2.filter(filesInScope, new Predicate<JetFile>() {
            @Override
            public boolean apply(@Nullable JetFile file) {
                return file != null
                       && file.getName().equals(fileName)
                       && JetPsiUtil.getFQName(file).equals(packageFqName);
            }
        });

        if (files.isEmpty()) return null;

        JetFile anyFile = files.iterator().next();
        if (files.size() == 1) {
            return anyFile;
        }

        Collection<JetFile> allNamespaceFiles = filesProvider.allNamespaceFiles().fun(anyFile);
        JetFile file = PsiCodegenPredictor.getFileForNamespacePartName(allNamespaceFiles, className);
        if (file != null) {
            return file;
        }

        // In the rare case that there's more than one file with this name in this package,
        // we may actually need to analyze the project in order to find a file which produces this class
        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeWithCache.analyzeFileWithCache(anyFile);

        return PsiCodegenPredictor.getFileForCodegenNamedClass(analyzeExhaust.getBindingContext(), allNamespaceFiles, className.getInternalName());
    }

    @NotNull
    private static FqName getPackageFqNameForClass(@NotNull JvmClassName className) {
        String internalName = className.getInternalName();
        int lastSlash = internalName.lastIndexOf('/');
        return lastSlash == -1 ? FqName.ROOT : new FqName(internalName.substring(0, lastSlash).replace('/', '.'));
    }
}
