package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.psi.JetElement;

/**
 * @author abreslav
 */
public interface DeclarationDescriptor<T extends JetElement> {
    T getPsiElement();
}
