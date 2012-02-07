package org.jetbrains.jet.asJava;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.PsiPackageImpl;

/**
 * TODO: make more accurate wrapper
 *
 * @author Nikolay Krasko
 */
public class JetLightPackage extends PsiPackageImpl {

    private final PsiElement namespaceElement;

    public JetLightPackage(PsiManager manager, String qualifiedName, PsiElement namespaceElement) {
        super(manager, qualifiedName);
        this.namespaceElement = namespaceElement;
    }

    @Override
    public PsiElement copy() {
        return new JetLightPackage(getManager(), getQualifiedName(), namespaceElement);
    }

    @Override
    public boolean isValid() {
        return namespaceElement.isValid();
    }
}