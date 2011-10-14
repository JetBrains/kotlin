package org.jetbrains.jet.plugin.references;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.AnalyzerFacade;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.List;

/**
* @author yole
*/
class JetSimpleNameReference extends JetPsiReference {
    private final JetSimpleNameExpression myExpression;

    public JetSimpleNameReference(JetSimpleNameExpression jetSimpleNameExpression) {
        super(jetSimpleNameExpression);
        myExpression = jetSimpleNameExpression;
    }

    @Override
    public PsiElement getElement() {
        return myExpression.getReferencedNameElement();
    }

    @Override
    public TextRange getRangeInElement() {
        PsiElement element = getElement();
        if (element == null) return null;
        return new TextRange(0, element.getTextLength());
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        PsiElement parent = myExpression.getParent();
        if (parent instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) parent;
            JetExpression receiverExpression = qualifiedExpression.getReceiverExpression();
            JetFile file = (JetFile) myExpression.getContainingFile();
            BindingContext bindingContext = AnalyzerFacade.analyzeFileWithCache(file);
            final JetType expressionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, receiverExpression);
            if (expressionType != null) {
                return collectLookupElements(bindingContext, expressionType.getMemberScope());
            }
        }
        else {
            JetFile file = (JetFile) myExpression.getContainingFile();
            BindingContext bindingContext = AnalyzerFacade.analyzeFileWithCache(file);
            JetScope resolutionScope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, myExpression);
            if (resolutionScope != null) {
                return collectLookupElements(bindingContext, resolutionScope);
            }
        }

        return EMPTY_ARRAY;
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        PsiElement element = JetPsiFactory.createNameIdentifier(myExpression.getProject(), newElementName);
        return myExpression.getReferencedNameElement().replace(element);
    }

    private Object[] collectLookupElements(BindingContext bindingContext, JetScope scope) {
        List<LookupElement> result = Lists.newArrayList();
        for (final DeclarationDescriptor descriptor : scope.getAllDescriptors()) {
            PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor.getOriginal());
            LookupElementBuilder element = LookupElementBuilder.create(descriptor.getName());
            String typeText = "";
            String tailText = "";
            boolean tailTextGrayed = false;
            if (descriptor instanceof FunctionDescriptor) {
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
                JetType returnType = functionDescriptor.getReturnType();
                typeText = DescriptorRenderer.TEXT.renderType(returnType);
                tailText = "(" + StringUtil.join(functionDescriptor.getValueParameters(), new Function<ValueParameterDescriptor, String>() {
                    @Override
                    public String fun(ValueParameterDescriptor valueParameterDescriptor) {
                        return valueParameterDescriptor.getName() + ":" +
                               DescriptorRenderer.TEXT.renderType(valueParameterDescriptor.getOutType());
                    }
                }, ",") + ")";
            }
            else if (descriptor instanceof VariableDescriptor) {
                JetType outType = ((VariableDescriptor) descriptor).getOutType();
                typeText = DescriptorRenderer.TEXT.renderType(outType);
            }
            else if (descriptor instanceof ClassDescriptor) {
                tailText = " (" + DescriptorRenderer.getFQName(descriptor.getContainingDeclaration()) + ")";
                tailTextGrayed = true;
            }
            else {
                typeText = DescriptorRenderer.TEXT.render(descriptor);
            }
            element = element.setTailText(tailText, tailTextGrayed).setTypeText(typeText);
            if (declaration != null) {
                element = element.setIcon(declaration.getIcon(Iconable.ICON_FLAG_OPEN | Iconable.ICON_FLAG_VISIBILITY));
            }
            result.add(element);
        }
        return result.toArray();
    }
}
