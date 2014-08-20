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

package org.jetbrains.jet.plugin.hierarchy;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.ArrayUtil;
import kotlin.Function1;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.plugin.ProjectRootsUtil;

public class HierarchyUtils {
    public static final Function1<PsiElement, Boolean> IS_CALL_HIERARCHY_ELEMENT = new Function1<PsiElement, Boolean>() {
        @Override
        public Boolean invoke(@Nullable PsiElement input) {
            return input instanceof PsiMethod ||
                   input instanceof PsiClass ||
                   input instanceof JetFile ||
                   input instanceof JetNamedFunction ||
                   input instanceof JetClassOrObject ||
                   input instanceof JetProperty;
        }
    };

    public static final Function1<PsiElement, Boolean> IS_OVERRIDE_HIERARCHY_ELEMENT = new Function1<PsiElement, Boolean>() {
        @Override
        public Boolean invoke(@Nullable PsiElement input) {
            return input instanceof PsiMethod ||
                   input instanceof JetNamedFunction ||
                   input instanceof JetProperty;
        }
    };

    public static PsiElement getCurrentElement(DataContext dataContext, Project project) {
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor != null) {
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file == null) return null;

            if (!ProjectRootsUtil.isInSource(file)) return null;
            if (JetPluginUtil.isKtFileInGradleProjectInWrongFolder(file)) return null;

            return TargetElementUtilBase.findTargetElement(editor, TargetElementUtilBase.getInstance().getAllAccepted());
        }

        return CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    }

    public static PsiElement getCallHierarchyElement(PsiElement element) {
        //noinspection unchecked
        return PsiUtilPackage.getParentByTypesAndPredicate(element, false, ArrayUtil.EMPTY_CLASS_ARRAY, IS_CALL_HIERARCHY_ELEMENT);
    }

    public static PsiElement getOverrideHierarchyElement(PsiElement element) {
        //noinspection unchecked
        return PsiUtilPackage.getParentByTypesAndPredicate(element, false, ArrayUtil.EMPTY_CLASS_ARRAY, IS_OVERRIDE_HIERARCHY_ELEMENT);
    }
}