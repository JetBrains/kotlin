package org.jetbrains.jet.lang.descriptors;

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
}
