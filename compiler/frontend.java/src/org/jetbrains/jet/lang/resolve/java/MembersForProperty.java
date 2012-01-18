package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiType;

/**
* @author Stepan Koltsov
*/
class MembersForProperty {
    PsiFieldWrapper field;
    PsiMethodWrapper setter;
    PsiMethodWrapper getter;

    PsiType type;
    PsiType receiverType;
}
