package org.jetbrains.jet.plugin.search;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.search.MethodTextOccurrenceProcessor;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.JetNamedFunction;

public class KotlinLightMethodTextOccurrenceProcessor extends MethodTextOccurrenceProcessor {
    public KotlinLightMethodTextOccurrenceProcessor(@NotNull PsiClass aClass, boolean strictSignatureSearch, PsiMethod... methods) {
        super(aClass, strictSignatureSearch, methods);
    }

    @Override
    protected boolean processInexactReference(
            PsiReference ref, PsiElement refElement, PsiMethod method, Processor<PsiReference> consumer
    ) {
        if (refElement instanceof JetNamedFunction) {
            PsiMethod lightMethod = LightClassUtil.getLightClassMethod((JetNamedFunction) refElement);
            if (lightMethod != null) return super.processInexactReference(ref, lightMethod, method, consumer);
        }

        return true;
    }
}
