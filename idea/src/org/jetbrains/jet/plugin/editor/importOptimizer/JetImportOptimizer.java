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

package org.jetbrains.jet.plugin.editor.importOptimizer;

import com.google.common.collect.Lists;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.JavaResolverPsiUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.*;

import static org.jetbrains.jet.plugin.quickfix.ImportInsertHelper.doNeedImport;

public class JetImportOptimizer implements ImportOptimizer {
    @Override
    public boolean supports(PsiFile file) {
        return file instanceof JetFile;
    }

    @NotNull
    @Override
    public Runnable processFile(final PsiFile file) {
        return new Runnable() {

            @Override
            public void run() {
                final JetFile jetFile = (JetFile) file;
                final Set<FqName> usedQualifiedNames = extractUsedQualifiedNames(jetFile);

                final List<JetImportDirective> directives = jetFile.getImportDirectives();

                final List<JetImportDirective> directivesBeforeCurrent = Lists.newArrayList();
                final List<JetImportDirective> directivesAfterCurrent = jetFile.getImportDirectives();

                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        // Remove only unnecessary imports
                        for (JetImportDirective anImport : directives) {
                            directivesAfterCurrent.remove(anImport);

                            ImportPath importPath = JetPsiUtil.getImportPath(anImport);
                            if (importPath == null) {
                                continue;
                            }

                            if (isUseful(importPath, usedQualifiedNames) &&
                                    doNeedImport(importPath, jetFile, directivesBeforeCurrent) &&
                                    doNeedImport(importPath, jetFile, directivesAfterCurrent)) {
                                directivesBeforeCurrent.add(anImport);
                            }
                            else {
                               anImport.delete();
                            }
                        }
                    }
                });
            }
        };
    }

    public static boolean isUseful(ImportPath importPath, Collection<FqName> usedNames) {
        if (importPath.hasAlias()) {
            // TODO: Add better analysis for aliases
            return true;
        }

        for (FqName usedName : usedNames) {
            if (QualifiedNamesUtil.isImported(importPath, usedName)) {
                return true;
            }
        }

        return false;
    }

    public static Set<FqName> extractUsedQualifiedNames(JetFile jetFile) {
        final Set<FqName> usedQualifiedNames = new HashSet<FqName>();
        jetFile.accept(new JetVisitorVoid() {
            @Override
            public void visitElement(PsiElement element) {
                ProgressIndicatorProvider.checkCanceled();
                element.acceptChildren(this);
            }

            @Override
            public void visitUserType(@NotNull JetUserType type) {
                if (type.getQualifier() == null) {
                    super.visitUserType(type);
                }
                else {
                    JetTypeArgumentList argumentList = type.getTypeArgumentList();
                    if (argumentList != null) {
                        super.visitTypeArgumentList(argumentList);
                    }
                    visitUserType(type.getQualifier());
                }
            }

            @Override
            public void visitReferenceExpression(@NotNull JetReferenceExpression expression) {
                if (PsiTreeUtil.getParentOfType(expression, JetImportDirective.class) == null &&
                        PsiTreeUtil.getParentOfType(expression, JetNamespaceHeader.class) == null) {

                    PsiReference reference = expression.getReference();
                    if (reference != null) {
                        List<PsiElement> references = new ArrayList<PsiElement>();
                        PsiElement resolve = reference.resolve();
                        if (resolve != null) {
                            references.add(resolve);
                        }

                        if (references.isEmpty() && reference instanceof PsiPolyVariantReference) {
                            for (ResolveResult resolveResult : ((PsiPolyVariantReference)reference).multiResolve(true)) {
                                references.add(resolveResult.getElement());
                            }
                        }

                        for (PsiElement psiReference : references) {
                            FqName fqName = getElementUsageFQName(psiReference);
                            if (fqName != null) {
                                usedQualifiedNames.add(fqName);
                            }
                        }
                    }
                }

                super.visitReferenceExpression(expression);
            }

            @Override
            public void visitForExpression(@NotNull JetForExpression expression) {
                BindingContext context = AnalyzerFacadeWithCache.getContextForElement(expression);
                ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, expression.getLoopRange());
                addResolvedCallFqName(resolvedCall);

                super.visitForExpression(expression);
            }

            @Override
            public void visitMultiDeclaration(@NotNull JetMultiDeclaration declaration) {
                BindingContext context = AnalyzerFacadeWithCache.getContextForElement(declaration);
                List<JetMultiDeclarationEntry> entries = declaration.getEntries();
                for (JetMultiDeclarationEntry entry : entries) {
                    ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
                    addResolvedCallFqName(resolvedCall);
                }

                super.visitMultiDeclaration(declaration);
            }

            private void addResolvedCallFqName(@Nullable ResolvedCall resolvedCall) {
                if (resolvedCall != null) {
                    CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
                    usedQualifiedNames.add(DescriptorUtils.getFqNameSafe(resultingDescriptor));
                }
            }
        });

        return usedQualifiedNames;
    }


    @Nullable
    public static FqName getElementUsageFQName(PsiElement element) {
        if (element instanceof JetFile) {
            return JetPsiUtil.getFQName((JetFile) element);
        }

        if (element instanceof JetSimpleNameExpression) {
            JetNamespaceHeader namespaceHeader = PsiTreeUtil.getParentOfType(element, JetNamespaceHeader.class);
            if (namespaceHeader != null) {
                List<JetSimpleNameExpression> simpleNameExpressions = PsiTreeUtil.getChildrenOfTypeAsList(namespaceHeader, JetSimpleNameExpression.class);
                FqName fqName = null;
                for (JetSimpleNameExpression nameExpression : simpleNameExpressions) {
                    Name referencedName = nameExpression.getReferencedNameAsName();
                    if (fqName == null) {
                        fqName = new FqName(referencedName.asString());
                    } else {
                        fqName = QualifiedNamesUtil.combine(fqName, referencedName);
                    }
                    if (nameExpression.equals(element)) {
                        return fqName;
                    }
                }
            }
        }

        if (element instanceof JetNamedDeclaration) {
            return JetPsiUtil.getFQName((JetNamedDeclaration) element);
        }

        if (element instanceof PsiClass) {
            String qualifiedName = ((PsiClass) element).getQualifiedName();
            if (qualifiedName != null) {
                return new FqName(qualifiedName);
            }
        }

        if (element instanceof PsiField) {
            PsiField field = (PsiField) element;

            FqName classFQN = getFqNameOfContainingClassForPsiMember(field);
            if (classFQN == null) {
                return null;
            }

            return combineClassFqNameWithMemberName(field.getContainingClass(), classFQN, field.getName());
        }

        // TODO: Still problem with kotlin global properties imported from class files
        if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element;

            FqName classFQN = getFqNameOfContainingClassForPsiMember(method);
            if (classFQN == null) {
                return null;
            }
            if (method.isConstructor()) {
                return classFQN;
            }

            return combineClassFqNameWithMemberName(method.getContainingClass(), classFQN, method.getName());
        }

        if (element instanceof PsiPackage) {
            return new FqName(((PsiPackage) element).getQualifiedName());
        }

        return null;
    }

    @Nullable
    private static FqName combineClassFqNameWithMemberName(PsiClass containingClass, FqName classFQN, String memberName) {
        if (memberName == null) {
            return null;
        }
        if (JavaResolverPsiUtils.isCompiledKotlinPackageClass(containingClass)) {
            return QualifiedNamesUtil.combine(classFQN.parent(), Name.identifier(memberName));
        }
        else {
            return QualifiedNamesUtil.combine(classFQN, Name.identifier(memberName));
        }
    }

    @Nullable
    private static FqName getFqNameOfContainingClassForPsiMember(PsiMember member) {
        PsiClass containingClass = member.getContainingClass();
        if (containingClass != null) {
            String classFQNStr = containingClass.getQualifiedName();
            if (classFQNStr != null) {
                return new FqName(classFQNStr);
            }
        }
        return null;
    }

    private static PsiElement getWithPreviousWhitespaces(PsiElement element) {
        PsiElement result = element;

        PsiElement siblingIterator = element.getPrevSibling();
        while (siblingIterator != null) {
            if (siblingIterator.getNode().getElementType() != TokenType.WHITE_SPACE) {
                break;
            }
            else {
                result = siblingIterator;
            }

            siblingIterator = siblingIterator.getPrevSibling();
        }

        return result;
    }

    private static PsiElement getWithFollowedWhitespaces(PsiElement element) {
        PsiElement result = element;

        PsiElement siblingIterator = element.getNextSibling();
        while (siblingIterator != null) {
            if (siblingIterator.getNode().getElementType() != TokenType.WHITE_SPACE) {
                break;
            }
            else {
                result = siblingIterator;
            }

            siblingIterator = siblingIterator.getNextSibling();
        }

        return result;
    }
}
