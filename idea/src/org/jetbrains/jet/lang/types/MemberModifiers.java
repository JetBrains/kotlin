package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public class MemberModifiers {

    private final boolean isAbstract;
    private final boolean isVirtual;
    private final boolean isOverride;

    public MemberModifiers(boolean isAbstract, boolean isVirtual, boolean isOverride) {
        this.isAbstract = isAbstract;
        this.isVirtual = isVirtual;
        this.isOverride = isOverride;
    }

    public boolean isAbstract() {
        return isAbstract;
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
}
