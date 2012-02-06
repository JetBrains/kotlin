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
    public JetLightPackage(PsiManager manager, String qualifiedName) {
        super(manager, qualifiedName);
    }

    @Override
    public PsiElement copy() {
        return new JetLightPackage(getManager(), getQualifiedName());
    }

    @Override
    public boolean isValid() {
        // TODO: invalidate properly
        return super.isValid();
    }
}