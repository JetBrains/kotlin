package org.jetbrains.jet.asJava;

import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.PsiPackageImpl;

/**
 * @author Nikolay Krasko
 */
public class JetLightPackage extends PsiPackageImpl {
    public JetLightPackage(PsiManager manager, String qualifiedName) {
        super(manager, qualifiedName);
    }

    @Override
    public boolean isValid() {
        return true;
    }
}