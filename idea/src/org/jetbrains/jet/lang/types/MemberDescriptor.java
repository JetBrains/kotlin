package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public interface MemberDescriptor {
    boolean isAbstract();
    boolean isVirtual();
    boolean isOverride();
}
