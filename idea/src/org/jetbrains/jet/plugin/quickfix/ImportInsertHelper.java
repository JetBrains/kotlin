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
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.java.JavaBridgeConfiguration;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.project.ProjectStructureUtil;
import org.jetbrains.jet.plugin.references.JetPsiReference;
import org.jetbrains.jet.util.QualifiedNamesUtil;
import org.jetbrains.k2js.analyze.JsConfiguration;

import java.util.List;

public class ImportInsertHelper {
    private ImportInsertHelper() {
    }

    /**
     * Add import directive corresponding to a type to file when it is needed.
     *
     * @param type type to import
     * @param file file where import directive should be added
     */
    public static void addImportDirectivesIfNeeded(@NotNull JetType type, @NotNull JetFile file) {
        if (JetPluginUtil.checkTypeIsStandard(type, file.getProject()) || type.isError()) {
            return;
        }
        BindingContext bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache(file).getBindingContext();
        PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, type.getMemberScope().getContainingDeclaration());
        if (element != null && element.getContainingFile() == file) { //declaration is in the same file, so no import is needed
            return;
        }
        for (ClassDescriptor clazz : TypeUtils.getAllClassDescriptors(type)) {
            addImportDirectiveIfNeeded(DescriptorUtils.getFQName(getTopLevelClass(clazz)).toSafe(), file);
        }
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

        if (isImportedWithKotlinDefault(importPath)) return true;

        if (ProjectStructureUtil.isJsKotlinModule(jetFile)) {
            return isImportedWithJsDefault(importPath);
        }
        else {
            return isImportedWithJavaDefault(importPath);
        }
    }

    public static boolean isImportedWithJavaDefault(ImportPath importPath) {
        return QualifiedNamesUtil.isImported(JavaBridgeConfiguration.DEFAULT_JAVA_IMPORTS, importPath);
    }

    public static boolean isImportedWithJsDefault(ImportPath importPath) {
        return QualifiedNamesUtil.isImported(JsConfiguration.DEFAULT_IMPORT_PATHS, importPath);
    }

    public static boolean isImportedWithKotlinDefault(ImportPath importPath) {
        return QualifiedNamesUtil.isImported(DefaultModuleConfiguration.DEFAULT_JET_IMPORTS, importPath);
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
