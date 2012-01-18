package org.jetbrains.jet.lang.resolve.java;

/**
* @author Stepan Koltsov
*/
class MembersForProperty {
    PsiFieldWrapper field;
    PsiMethodWrapper setter;
    PsiMethodWrapper getter;

    TypeSource type;
    TypeSource receiverType;
}
