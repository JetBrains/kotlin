package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author svtk
 */
public class RemovePartsFromPropertyFix extends JetIntentionAction<JetProperty> {
    private final JetType type;
    private final String partsToRemove;
    
    private RemovePartsFromPropertyFix(@NotNull JetProperty element, JetType type) {
        super(element);
        this.type = type;
        partsToRemove = partsToRemove(element.getGetter() != null && element.getGetter().getBodyExpression() != null,
                                      element.getSetter() != null && element.getSetter().getBodyExpression() != null,
                                      element.getInitializer() != null);
    }

    private static String partsToRemove(boolean hasGetter, boolean hasSetter, boolean hasInitializer) {
        StringBuilder sb = new StringBuilder();
        if (hasGetter) {
            sb.append("getter");
            if (hasSetter && hasInitializer) {
                sb.append(", ");
            }
            else if (hasSetter || hasInitializer) {
                sb.append(" and ");
            }
        }
        if (hasSetter) {
            sb.append("setter");
            if (hasInitializer) {
                sb.append(" and ");
            }
        }
        if (hasInitializer) {
            sb.append("initializer");
        }
        return sb.toString();
    }

    @NotNull
    @Override
    public String getText() {
        return "Remove " + partsToRemove + " from property";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Remove parts from property to make it abstract";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return element.isValid();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetProperty newElement = (JetProperty) element.copy();
        JetPropertyAccessor getter = newElement.getGetter();
        if (getter != null) {
            newElement.deleteChildInternal(getter.getNode());
        }
        JetPropertyAccessor setter = newElement.getSetter();
        if (setter != null) {
            newElement.deleteChildInternal(setter.getNode());
        }
        JetExpression initializer = newElement.getInitializer();
        boolean needImport = false;
        if (initializer != null) {
            PsiElement nameIdentifier = newElement.getNameIdentifier();
            assert nameIdentifier != null;
            PsiElement nextSibling = nameIdentifier.getNextSibling();
            assert nextSibling != null;
            newElement.deleteChildRange(nextSibling, initializer);

            if (newElement.getPropertyTypeRef() == null) {
                newElement = addPropertyType(project, newElement, type);
                needImport = true;
            }
        }
        if (needImport) {
            ImportClassHelper.perform(type, element, newElement);
        } else {
            element.replace(newElement);
        }
    }

    public static JetProperty addPropertyType(Project project, JetProperty property, JetType type) {
        JetProperty newProperty = (JetProperty) property.copy();
        JetTypeReference typeReference = JetPsiFactory.createType(project, type.toString());
        PsiElement[] colon = JetPsiFactory.createColon(project);
        PsiElement nameIdentifier = newProperty.getNameIdentifier();
        assert nameIdentifier != null;
        newProperty.addAfter(typeReference, nameIdentifier);
        for (int i = colon.length - 1; i >= 0; i--) {
            newProperty.addAfter(colon[i], nameIdentifier);
        }
        return newProperty;
    }

    public static JetIntentionActionFactory<JetProperty> createFactory() {
        return new JetIntentionActionFactory<JetProperty>() {
            @Override
            public JetIntentionAction<JetProperty> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetProperty;
                DiagnosticWithParameters<PsiElement> diagnosticWithParameters = assertAndCastToDiagnosticWithParameters(diagnostic, DiagnosticParameters.TYPE);
                JetType type = diagnosticWithParameters.getParameter(DiagnosticParameters.TYPE);
                return new RemovePartsFromPropertyFix((JetProperty) diagnostic.getPsiElement(), type);
            }
        };
    }
}
