package org.jetbrains.jet.lang.descriptors;

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


}
