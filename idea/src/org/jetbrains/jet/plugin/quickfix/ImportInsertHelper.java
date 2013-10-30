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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.project.ProjectStructureUtil;
import org.jetbrains.jet.plugin.references.JetPsiReference;
import org.jetbrains.jet.util.QualifiedNamesUtil;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;

import java.util.List;

public class ImportInsertHelper {
    private ImportInsertHelper() {
    }

    /**
     * Add import directive into the PSI tree for the given namespace.
     *
     * @param importFqn full name of the import
     * @param file File where directive should be added.
     */
    public static void addImportDirectiveIfNeeded(@NotNull FqName importFqn, @NotNull JetFile file) {
        addImportDirectiveIfNeeded(new ImportPath(importFqn, false), file);
    }

    public static void addImportDirectiveOrChangeToFqName(@NotNull FqName importFqn, @NotNull JetFile file, int refOffset, @NotNull PsiElement targetElement) {
        PsiReference reference = file.findReferenceAt(refOffset);
        if (reference instanceof JetPsiReference) {
            PsiElement target = reference.resolve();
            if (target != null) {
                boolean same = file.getManager().areElementsEquivalent(target, targetElement);

                if (!same) {
                    same = target instanceof PsiClass && importFqn.asString().equals(((PsiClass)target).getQualifiedName());
                }

                if (!same) {
                    if (target instanceof PsiMethod) {
                        PsiMethod method = (PsiMethod) target;
                        same = (method.isConstructor() && file.getManager().areElementsEquivalent(method.getContainingClass(), targetElement));
                    }
                }

                if (!same) {
                    if (target instanceof JetObjectDeclarationName) {
                        same = file.getManager().areElementsEquivalent(target.getParent(), targetElement);
                    }
                }

                if (!same) {
                    Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
                    TextRange refRange = reference.getElement().getTextRange();
                    document.replaceString(refRange.getStartOffset(), refRange.getEndOffset(), importFqn.asString());
                }
                return;
            }
        }
        addImportDirectiveIfNeeded(new ImportPath(importFqn, false), file);
    }

    public static void addImportDirectiveIfNeeded(@NotNull ImportPath importPath, @NotNull JetFile file) {
        if (CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY) {
            new OptimizeImportsProcessor(file.getProject(), file).runWithoutProgress();
        }

        if (!doNeedImport(importPath, file)) {
            return;
        }

        writeImportToFile(importPath, file);
    }

    public static void writeImportToFile(@NotNull ImportPath importPath, @NotNull JetFile file) {
        JetImportList importList = file.getImportList();
        if (importList != null) {
            JetImportDirective newDirective = JetPsiFactory.createImportDirective(file.getProject(), importPath);
            importList.add(newDirective);
        }
        else {
            JetImportList newDirective = JetPsiFactory.createImportDirectiveWithImportList(file.getProject(), importPath);
            JetNamespaceHeader header = file.getNamespaceHeader();
            if (header == null) {
                throw new IllegalStateException("Scripts are not supported: " + file.getName());
            }

            header.getParent().addAfter(newDirective, header);
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
            if (QualifiedNamesUtil.isOneSegmentFQN(importPath.fqnPart())) {
                return true;
            }

            // There's no need to import a declaration from the package of current file
            if (JetPsiUtil.getFQName(jetFile).equals(importPath.fqnPart().parent())) {
                return true;
            }
        }

        return isImportedWithDefault(importPath, jetFile);
    }

    public static boolean isImportedWithDefault(@NotNull ImportPath importPath, @NotNull JetFile contextFile) {
        List<ImportPath> defaultImports = ProjectStructureUtil.isJsKotlinModule(contextFile)
                                   ? AnalyzerFacadeForJS.DEFAULT_IMPORTS
                                   : AnalyzerFacadeForJVM.DEFAULT_IMPORTS;
        return QualifiedNamesUtil.isImported(defaultImports, importPath);
    }

    public static boolean doNeedImport(@NotNull ImportPath importPath, @NotNull JetFile file) {
        return doNeedImport(importPath, file, file.getImportDirectives());
    }

    public static boolean doNeedImport(@NotNull ImportPath importPath, @NotNull JetFile file, List<JetImportDirective> importDirectives) {
        if (importPath.fqnPart().firstSegmentIs(JavaDescriptorResolver.JAVA_ROOT)) {
            FqName withoutJavaRoot = QualifiedNamesUtil.withoutFirstSegment(importPath.fqnPart());
            importPath = new ImportPath(withoutJavaRoot, importPath.isAllUnder(), importPath.getAlias());
        }

        if (isImportedByDefault(importPath, file)) {
            return false;
        }

        if (!importDirectives.isEmpty()) {
            // Check if import is already present
            for (JetImportDirective directive : importDirectives) {
                ImportPath existentImportPath = JetPsiUtil.getImportPath(directive);
                if (existentImportPath != null && QualifiedNamesUtil.isImported(existentImportPath, importPath)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static ClassDescriptor getTopLevelClass(ClassDescriptor classDescriptor) {
        while (true) {
            DeclarationDescriptor parent = classDescriptor.getContainingDeclaration();
            if (parent instanceof ClassDescriptor) {
                classDescriptor = (ClassDescriptor) parent;
            } else {
                return classDescriptor;
            }
        }
    }
}
