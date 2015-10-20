/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.hierarchy;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.ide.hierarchy.type.JavaTypeHierarchyProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.decompiler.navigation.JetSourceNavigationHelper;
import org.jetbrains.kotlin.idea.stubindex.JetClassShortNameIndex;
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collection;

public class KotlinTypeHierarchyProvider extends JavaTypeHierarchyProvider {
    @Override
    public PsiElement getTarget(@NotNull DataContext dataContext) {
        Project project = PlatformDataKeys.PROJECT.getData(dataContext);
        if (project == null) return null;

        Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
        if (editor != null) {
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file == null) return null;

            if (!ProjectRootsUtil.isInProjectOrLibSource(file)) return null;

            PsiElement target = TargetElementUtilBase.findTargetElement(editor, TargetElementUtilBase.getInstance().getAllAccepted());

            if (target instanceof PsiClass) {
                return target;
            }

            if (target instanceof KtClassOrObject) {
                return JetSourceNavigationHelper.getOriginalPsiClassOrCreateLightClass((KtClassOrObject) target);
            }
            // Factory methods
            else if (target instanceof KtNamedFunction) {
                KtNamedFunction function = (KtNamedFunction) target;
                String functionName = function.getName();
                FunctionDescriptor functionDescriptor = ResolutionUtils.analyze(function)
                        .get(BindingContext.FUNCTION, target);
                if (functionDescriptor != null) {
                    KotlinType type = functionDescriptor.getReturnType();
                    if (type != null) {
                        String returnTypeText = DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type);
                        if (returnTypeText.equals(functionName)) {
                            Collection<KtClassOrObject> classOrObjects =
                                    JetClassShortNameIndex.getInstance().get(functionName, project, GlobalSearchScope.allScope(project));
                            if (classOrObjects.size() == 1) {
                                KtClassOrObject classOrObject = classOrObjects.iterator().next();
                                return JetSourceNavigationHelper.getOriginalPsiClassOrCreateLightClass(classOrObject);
                            }
                        }
                    }
                }
            }

            int offset = editor.getCaretModel().getOffset();
            PsiElement element = file.findElementAt(offset);
            if (element == null) return null;

            KtClassOrObject classOrObject = PsiTreeUtil.getParentOfType(element, KtClassOrObject.class);
            if (classOrObject != null) {
                return JetSourceNavigationHelper.getOriginalPsiClassOrCreateLightClass(classOrObject);
            }
        }
        else {
            PsiElement element = LangDataKeys.PSI_ELEMENT.getData(dataContext);
            if (element instanceof KtClassOrObject) {
                return JetSourceNavigationHelper.getOriginalPsiClassOrCreateLightClass((KtClassOrObject) element);
            }
        }

        return null;
    }
}

