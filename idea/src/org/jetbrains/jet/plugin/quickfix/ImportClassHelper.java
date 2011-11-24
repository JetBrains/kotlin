package org.jetbrains.jet.plugin.quickfix;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetPluginUtil;

import java.util.List;

/**
 * @author svtk
 */
public class ImportClassHelper {
    public static void perform(@NotNull JetType type, @NotNull PsiElement elementToReplace, @NotNull PsiElement newElement) {
        if (JetPluginUtil.checkTypeIsStandard(type, elementToReplace.getProject()) || ErrorUtils.isError(type.getMemberScope().getContainingDeclaration())) {
            elementToReplace.replace(newElement);
            return;
        }
        perform(JetPluginUtil.computeTypeFullName(type), elementToReplace, newElement);
    }
    
    public static void perform(@NotNull String typeFullName, @NotNull JetNamespace namespace) {
        perform(typeFullName, namespace, null, null);
    }

    public static void perform(@NotNull String typeFullName, @NotNull PsiElement elementToReplace, @NotNull PsiElement newElement) {
        PsiElement parent = elementToReplace;
        while (!(parent instanceof JetNamespace)) {
            parent = parent.getParent();
            assert parent != null;
        }
        perform(typeFullName, (JetNamespace) parent, elementToReplace, newElement);
    }

    public static void perform(@NotNull String typeFullName, @NotNull JetNamespace namespace, @Nullable PsiElement elementToReplace, @Nullable PsiElement newElement) {
        List<JetImportDirective> importDirectives = namespace.getImportDirectives();

        JetImportDirective newDirective = JetPsiFactory.createImportDirective(namespace.getProject(), typeFullName);

        String lineSeparator = System.getProperty("line.separator");
        if (!importDirectives.isEmpty()) {
            boolean isPresent = false;
            for (JetImportDirective directive : importDirectives) {
                if (directive.getText().endsWith(typeFullName) ||
                    directive.getText().endsWith(typeFullName + ";")) {
                    isPresent = true;
                }
            }
            if (!isPresent) {
                JetImportDirective lastDirective = importDirectives.get(importDirectives.size() - 1);
                lastDirective.getParent().addAfter(newDirective, lastDirective);
                lastDirective.getParent().addAfter(JetPsiFactory.createWhiteSpace(namespace.getProject(), lineSeparator), lastDirective);
            }
        }
        else {
            List<JetDeclaration> declarations = namespace.getDeclarations();
            assert !declarations.isEmpty();
            JetDeclaration firstDeclaration = declarations.iterator().next();
            firstDeclaration.getParent().addBefore(newDirective, firstDeclaration);
            firstDeclaration.getParent().addBefore(JetPsiFactory.createWhiteSpace(namespace.getProject(), lineSeparator + lineSeparator), firstDeclaration);
        }
        if (elementToReplace != null && newElement != null && elementToReplace != newElement) {
            elementToReplace.replace(newElement);
        }
    }
}
