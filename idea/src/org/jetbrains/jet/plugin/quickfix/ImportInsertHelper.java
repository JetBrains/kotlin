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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.JavaBridgeConfiguration;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.plugin.references.JetPsiReference;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.List;

import static org.jetbrains.jet.plugin.project.AnalyzeSingleFileUtil.getContextForSingleFile;

/**
 * @author svtk
 */
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
        if (JetPluginUtil.checkTypeIsStandard(type, file.getProject()) || ErrorUtils.isErrorType(type)) {
            return;
        }
        BindingContext bindingContext = getContextForSingleFile(file);
        PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, type.getMemberScope().getContainingDeclaration());
        if (element != null && element.getContainingFile() == file) { //declaration is in the same file, so no import is needed
            return;
        }
        for (ClassDescriptor clazz : TypeUtils.getAllClassDescriptors(type)) {
            addImportDirective(DescriptorUtils.getFQName(getTopLevelClass(clazz)).toSafe(), file);
        }
    }

    /**
     * Add import directive into the PSI tree for the given namespace.
     *
     * @param importFqn full name of the import
     * @param file File where directive should be added.
     */
    public static void addImportDirective(@NotNull FqName importFqn, @NotNull JetFile file) {
        addImportDirective(new ImportPath(importFqn, false), null, file);
    }

    public static void addImportDirectiveOrChangeToFqName(@NotNull FqName importFqn, @NotNull JetFile file, int refOffset, @NotNull PsiElement targetElement) {
        PsiReference reference = file.findReferenceAt(refOffset);
        if (reference instanceof JetPsiReference) {
            PsiElement target = reference.resolve();
            if (target != null) {
                boolean same = file.getManager().areElementsEquivalent(target, targetElement);
                same |= target instanceof PsiClass && importFqn.getFqName().equals(((PsiClass)target).getQualifiedName());
                if (!same) {
                    Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
                    TextRange refRange = reference.getElement().getTextRange();
                    document.replaceString(refRange.getStartOffset(), refRange.getEndOffset(), importFqn.getFqName());
                }
                return;
            }
        }
        addImportDirective(new ImportPath(importFqn, false), null, file);
    }

    public static void addImportDirective(@NotNull ImportPath importPath, @Nullable String aliasName, @NotNull JetFile file) {
        if (!doNeedImport(importPath, aliasName, file)) {
            return;
        }

        JetImportDirective newDirective = JetPsiFactory.createImportDirective(file.getProject(), importPath, aliasName);
        List<JetImportDirective> importDirectives = file.getImportDirectives();

        if (!importDirectives.isEmpty()) {
            JetImportDirective lastDirective = importDirectives.get(importDirectives.size() - 1);
            lastDirective.getParent().addAfter(newDirective, lastDirective);
        }
        else {
            file.getNamespaceHeader().getParent().addAfter(newDirective, file.getNamespaceHeader());
        }
    }

    /**
     * Check that import is useless.
     */
    private static boolean isImportedByDefault(@NotNull ImportPath importPath, @Nullable String aliasName, @NotNull FqName filePackageFqn) {
        if (importPath.fqnPart().isRoot()) {
            return true;
        }

        if (aliasName != null) {
            return false;
        }

        // Single element import without .* and alias is useless
        if (!importPath.isAllUnder() && QualifiedNamesUtil.isOneSegmentFQN(importPath.fqnPart())) {
            return true;
        }

        // There's no need to import a declaration from the package of current file
        if (!importPath.isAllUnder() && filePackageFqn.equals(importPath.fqnPart().parent())) {
            return true;
        }

        for (ImportPath defaultJetImport : DefaultModuleConfiguration.DEFAULT_JET_IMPORTS) {
            if (QualifiedNamesUtil.isImported(defaultJetImport, importPath)) {
                return true;
            }
        }

        for (ImportPath defaultJavaImport : JavaBridgeConfiguration.DEFAULT_JAVA_IMPORTS) {
            if (QualifiedNamesUtil.isImported(defaultJavaImport, importPath)) {
                return true;
            }
        }

        return false;
    }

    public static boolean doNeedImport(@NotNull ImportPath importPath, @Nullable String aliasName, @NotNull JetFile file) {
        if (QualifiedNamesUtil.getFirstSegment(importPath.fqnPart().getFqName()).equals(JavaDescriptorResolver.JAVA_ROOT.getName())) {
            FqName withoutJavaRoot = QualifiedNamesUtil.withoutFirstSegment(importPath.fqnPart());
            importPath = new ImportPath(withoutJavaRoot, importPath.isAllUnder());
        }

        if (isImportedByDefault(importPath, null, JetPsiUtil.getFQName(file))) {
            return false;
        }

        List<JetImportDirective> importDirectives = file.getImportDirectives();

        if (!importDirectives.isEmpty()) {
            // Check if import is already present
            for (JetImportDirective directive : importDirectives) {
                ImportPath existentImportPath = JetPsiUtil.getImportPath(directive);
                if (directive.getAliasName() == null && aliasName == null) {
                    if (existentImportPath != null && QualifiedNamesUtil.isImported(existentImportPath, importPath)) {
                        return false;
                    }
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
