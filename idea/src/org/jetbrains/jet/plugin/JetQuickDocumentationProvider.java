package org.jetbrains.jet.plugin;

import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.DeclarationDescriptor;
import org.jetbrains.jet.resolve.DescriptorUtil;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class JetQuickDocumentationProvider implements DocumentationProvider {

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
        String text = DescriptorUtil.renderPresentableText(declarationDescriptor);
        text = text.replaceAll("<", "&lt;");
        return text;
    }

    @Override
    public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
        return Collections.emptyList();
    }

    @Override
    public String generateDoc(PsiElement element, PsiElement originalElement) {
        return "<no doc>";
    }

    @Override
    public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
        return null;
    }

    @Override
    public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
        return null;
    }
}
