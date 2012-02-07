package org.jetbrains.jet.plugin.refactoring;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * User: Alefas
 * Date: 07.02.12
 */
public class JetNameValidatorImpl implements JetNameValidator {
    public static JetNameValidator getEmptyValidator(final Project project) {
        return new JetNameValidator() {
            @Override
            public String validateName(String name) {
                return name;
            }

            @Override
            public Project getProject() {
                return project;
            }
        };
    }

    private final PsiElement myPlace;

    public JetNameValidatorImpl(PsiElement place) {
        myPlace = place;
    }

    @Nullable
    public String validateName(String name) {
        return name;
    }

    public Project getProject() {
        return myPlace.getProject();
    }
}
