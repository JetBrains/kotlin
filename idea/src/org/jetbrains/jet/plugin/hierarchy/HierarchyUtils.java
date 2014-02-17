package org.jetbrains.jet.plugin.hierarchy;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.ArrayUtil;
import jet.Function1;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.jet.plugin.JetPluginUtil;

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

            if (!JetPluginUtil.isInSource(file)) return null;
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
