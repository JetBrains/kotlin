package org.jetbrains.jet.plugin.refactoring.safeDelete;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.safeDelete.JavaSafeDeleteDelegate;
import com.intellij.usageView.UsageInfo;

import java.util.List;

public class KotlinJavaSafeDeleteDelegate implements JavaSafeDeleteDelegate {
    @Override
    public void createUsageInfoForParameter(PsiReference reference, List<UsageInfo> usages, PsiParameter parameter, PsiMethod method) {

    }
}
