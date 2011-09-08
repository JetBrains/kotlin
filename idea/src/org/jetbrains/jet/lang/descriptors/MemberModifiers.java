package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class MemberModifiers extends Modifiers {
    public static final MemberModifiers DEFAULT_MODIFIERS = new MemberModifiers(false, false, false);

    private final boolean isVirtual;
    private final boolean isOverride;

    public MemberModifiers(boolean isAbstract, boolean isVirtual, boolean isOverride) {
        super(isAbstract);
        this.isVirtual = isVirtual;
        this.isOverride = isOverride;
    }


    public boolean isVirtual() {
        return isVirtual;
    }

    public boolean isOverride() {
        return isOverride;
    }

    public boolean isOverridable() {
        return isAbstract() || isVirtual() || isOverride();
    }

    @NotNull
    public static MemberModifiers resolveModifiers(@Nullable JetModifierList modifierList) {
        return resolveModifiers(modifierList, DEFAULT_MODIFIERS);
    }

    @NotNull
    public static MemberModifiers resolveModifiers(@Nullable JetModifierList modifierList, @NotNull MemberModifiers defaultModifiers) {
        if (modifierList == null) return defaultModifiers;
        return new MemberModifiers(
                modifierList.hasModifier(JetTokens.ABSTRACT_KEYWORD),
                modifierList.hasModifier(JetTokens.VIRTUAL_KEYWORD),
                modifierList.hasModifier(JetTokens.OVERRIDE_KEYWORD)
        );
    }
}
