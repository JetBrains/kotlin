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
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.codegen.binding.PsiCodegenPredictor;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils;
import org.jetbrains.jet.plugin.debugger.DebuggerPackage;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.plugin.stubindex.PackageIndexUtil.findFilesWithExactPackage;

public class DebuggerUtils {
    private DebuggerUtils() {
    }

    @Nullable
    public static JetFile findSourceFileForClass(
            @NotNull Project project,
            @NotNull GlobalSearchScope searchScope,
            @NotNull final JvmClassName className,
            @NotNull final String fileName,
            final int lineNumber
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

        boolean isInLibrary = KotlinPackage.any(filesWithExactName, new Function1<JetFile, Boolean>() {
            @Override
            public Boolean invoke(JetFile file) {
                return LibraryUtil.findLibraryEntry(file.getVirtualFile(), file.getProject()) != null;
            }
        });

        if (isInLibrary) {
            return KotlinPackage.singleOrNull(KotlinPackage.filter(filesWithExactName, new Function1<JetFile, Boolean>() {
                @Override
                public Boolean invoke(JetFile file) {
                    Integer startLineOffset = CodeInsightUtils.getStartLineOffset(file, lineNumber);
                    assert startLineOffset != null : "Cannot find start line offset for file " + file.getName() + ", line " + lineNumber;
                    JetElement elementAt = PsiTreeUtil.getParentOfType(file.findElementAt(startLineOffset), JetElement.class);
                    return elementAt != null &&
                           className.getInternalName().equals(DebuggerPackage.findPackagePartInternalNameForLibraryFile(elementAt));
                }
            }));
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
