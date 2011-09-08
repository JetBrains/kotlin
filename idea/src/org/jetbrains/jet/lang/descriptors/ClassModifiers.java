package org.jetbrains.jet.lang.descriptors;

/**
 * @author svtk
 */
public class ClassModifiers extends Modifiers {
    public static final ClassModifiers DEFAULT_MODIFIERS = new ClassModifiers(false, false, false, false);

    private boolean open;
    private boolean trait;
    private boolean isEnum;

    public ClassModifiers(boolean anAbstract, boolean open, boolean trait, boolean isEnum) {
        super(anAbstract);
        this.open = open;
        this.trait = trait;
        this.isEnum = isEnum;
    }

    public boolean isOpen() {
        return open;
    }
    
    public boolean isTrait() {
        return trait;
    }
    
    public boolean isEnum() {
        return isEnum;
    }
}
