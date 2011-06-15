package org.jetbrains.jet.plugin;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.resolve.DescriptorRenderer;

/**
 * @author abreslav
 */
public class JetQuickDocumentationProvider extends AbstractDocumentationProvider {

    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        JetReferenceExpression ref;
        if (originalElement instanceof JetReferenceExpression) {
            ref = (JetReferenceExpression) originalElement;
        }
        else {
            ref = PsiTreeUtil.getParentOfType(originalElement, JetReferenceExpression.class);
        }
        if (ref != null) {
            BindingContext bindingContext = AnalyzingUtils.analyzeFileWithCache((JetFile) element.getContainingFile());
            DeclarationDescriptor declarationDescriptor = bindingContext.resolveReferenceExpression(ref);
            if (declarationDescriptor != null) {
                return render(declarationDescriptor);
            }
            PsiElement psiElement = bindingContext.resolveToDeclarationPsiElement(ref);
            if (psiElement != null) {
                declarationDescriptor = bindingContext.getDeclarationDescriptor(psiElement);
                if (declarationDescriptor != null) {
                    return render(declarationDescriptor);
                }
            }
            return "Unresolved";
        }

//        if (originalElement.getNode().getElementType() == JetTokens.IDENTIFIER) {
//            JetDeclaration declaration = PsiTreeUtil.getParentOfType(originalElement, JetDeclaration.class);
//            BindingContext bindingContext = AnalyzingUtils.analyzeFileWithCache((JetFile) element.getContainingFile(), ErrorHandler.DO_NOTHING);
//            DeclarationDescriptor declarationDescriptor = bindingContext.getDeclarationDescriptor(declaration);
//            if (declarationDescriptor != null) {
//                return render(declarationDescriptor);
//            }
//        }
        return "Not a reference";
    }

    private String render(DeclarationDescriptor declarationDescriptor) {
        return DescriptorRenderer.HTML.render(declarationDescriptor);
    }
}
