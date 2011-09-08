package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author svtk
 */
public class ClassModifiers extends Modifiers {
    public static final ClassModifiers DEFAULT_MODIFIERS = new ClassModifiers(false, false, false);

    private boolean open;
    private boolean trait;

    public ClassModifiers(boolean anAbstract, boolean open, boolean trait) {
        super(anAbstract);
        this.open = open;
        this.trait = trait;
    }

    public boolean isOpen() {
        return open;
    }
    
    public boolean isTrait() {
        return trait;
    }

    public static ClassModifiers resolveModifiers(@Nullable JetModifierList modifierList) {
        if (modifierList == null) return DEFAULT_MODIFIERS;
        return new ClassModifiers(
                modifierList.hasModifier(JetTokens.ABSTRACT_KEYWORD),
                modifierList.hasModifier(JetTokens.OPEN_KEYWORD) || modifierList.hasModifier(JetTokens.ABSTRACT_KEYWORD),
                modifierList.hasModifier(JetTokens.TRAIT_KEYWORD)
        );
    }

}
