package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PropertyAccessorDescriptor;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author Stepan Koltsov
 */
class PropertyAccessorData {

    @NotNull
    private final PsiMemberWrapper member;
    private final boolean getter;

    @NotNull
    private final TypeSource type;
    @Nullable
    private final TypeSource receiverType;
    
    
    PropertyAccessorData(@NotNull PsiMethodWrapper method, boolean getter, @NotNull TypeSource type, @Nullable TypeSource receiverType) {
        this.member = method;
        this.type = type;
        this.receiverType = receiverType;
        this.getter = getter;
    }

    PropertyAccessorData(@NotNull PsiFieldWrapper field, @NotNull TypeSource type, @Nullable TypeSource receiverType) {
        this.member = field;
        this.type = type;
        this.receiverType = receiverType;
        this.getter = false;
    }

    @NotNull
    public PsiMemberWrapper getMember() {
        return member;
    }

    @NotNull
    public TypeSource getType() {
        return type;
    }

    @Nullable
    public TypeSource getReceiverType() {
        return receiverType;
    }

    public boolean isGetter() {
        return member instanceof PsiMethodWrapper && getter;
    }

    public boolean isSetter() {
        return member instanceof PsiMethodWrapper && !getter;
    }

    public boolean isField() {
        return member instanceof PsiFieldWrapper;
    }
}
