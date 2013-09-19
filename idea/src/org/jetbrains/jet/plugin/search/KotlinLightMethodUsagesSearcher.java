package org.jetbrains.jet.plugin.search;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.search.MethodTextOccurrenceProcessor;
import com.intellij.psi.impl.search.MethodUsagesSearcher;

public class KotlinLightMethodUsagesSearcher extends MethodUsagesSearcher {
    @Override
    protected MethodTextOccurrenceProcessor getTextOccurrenceProcessor(
            PsiMethod[] methods, PsiClass aClass, boolean strictSignatureSearch
    ) {
        return new KotlinLightMethodTextOccurrenceProcessor(aClass, strictSignatureSearch, methods);
    }
}
