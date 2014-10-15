/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.java.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.NamePackage;
import org.jetbrains.jet.plugin.project.ProjectStructureUtil;
import org.jetbrains.k2js.analyze.TopDownAnalyzerFacadeForJS;

import java.util.List;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class ImportInsertHelper {
    private ImportInsertHelper() {
    }

    /**
     * Add import directive into the PSI tree for the given package.
     *
     * @param importFqn full name of the import
     * @param file File where directive should be added.
     */
    public static void addImportDirectiveIfNeeded(@NotNull FqName importFqn, @NotNull JetFile file) {
        ImportPath importPath = new ImportPath(importFqn, false);

        optimizeImportsOnTheFly(file);

        if (needImport(importPath, file)) {
            writeImportToFile(importPath, file);
        }
    }

    public static void optimizeImportsOnTheFly(JetFile file) {
        if (CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY) {
            new OptimizeImportsProcessor(file.getProject(), file).runWithoutProgress();
        }
    }

    public static void writeImportToFile(@NotNull ImportPath importPath, @NotNull JetFile file) {
        JetPsiFactory psiFactory = JetPsiFactory(file.getProject());
        if (file instanceof JetCodeFragment) {
            JetImportDirective newDirective = psiFactory.createImportDirective(importPath);
            ((JetCodeFragment) file).addImportsFromString(newDirective.getText());
            return;
        }

        JetImportList importList = file.getImportList();
        if (importList != null) {
            JetImportDirective newDirective = psiFactory.createImportDirective(importPath);
            importList.add(psiFactory.createNewLine());
            importList.add(newDirective);
        }
        else {
            JetImportList newDirective = psiFactory.createImportDirectiveWithImportList(importPath);
            JetPackageDirective packageDirective = file.getPackageDirective();
            if (packageDirective == null) {
                throw new IllegalStateException("Scripts are not supported: " + file.getName());
            }

            packageDirective.getParent().addAfter(newDirective, packageDirective);
        }
    }

    /**
     * Check that import is useless.
     */
    private static boolean isImportedByDefault(@NotNull ImportPath importPath, @NotNull JetFile jetFile) {
        if (importPath.fqnPart().isRoot()) {
            return true;
        }

        if (!importPath.isAllUnder() && !importPath.hasAlias()) {
            // Single element import without .* and alias is useless
            if (NamePackage.isOneSegmentFQN(importPath.fqnPart())) {
                return true;
            }

            // There's no need to import a declaration from the package of current file
            if (jetFile.getPackageFqName().equals(importPath.fqnPart().parent())) {
                return true;
            }
        }

        return isImportedWithDefault(importPath, jetFile);
    }

    public static boolean isImportedWithDefault(@NotNull ImportPath importPath, @NotNull JetFile contextFile) {
        List<ImportPath> defaultImports = ProjectStructureUtil.isJsKotlinModule(contextFile)
                                   ? TopDownAnalyzerFacadeForJS.DEFAULT_IMPORTS
                                   : TopDownAnalyzerFacadeForJVM.DEFAULT_IMPORTS;
        return NamePackage.isImported(importPath, defaultImports);
    }

    public static boolean needImport(@NotNull FqName fqName, @NotNull JetFile file) {
        return needImport(new ImportPath(fqName, false), file);
    }

    public static boolean needImport(@NotNull ImportPath importPath, @NotNull JetFile file) {
        return needImport(importPath, file, file.getImportDirectives());
    }

    public static boolean needImport(@NotNull ImportPath importPath, @NotNull JetFile file, List<JetImportDirective> importDirectives) {
        if (isImportedByDefault(importPath, file)) {
            return false;
        }

        if (!importDirectives.isEmpty()) {
            // Check if import is already present
            for (JetImportDirective directive : importDirectives) {
                ImportPath existentImportPath = directive.getImportPath();
                if (existentImportPath != null && NamePackage.isImported(importPath, existentImportPath)) {
                    return false;
                }
            }
        }

        return true;
    }
}
