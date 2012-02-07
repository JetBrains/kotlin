package org.jetbrains.jet.plugin.refactoring;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * User: Alefas
 * Date: 07.02.12
 */
public interface JetNameValidator {
    /**
     * Validates name, and slightly improves it by adding number to name in case of conflicts
     * @param name to check it in scope
     * @return name or nameI, where I is number
     */
    @Nullable
    String validateName(String name);

    Project getProject();
}
