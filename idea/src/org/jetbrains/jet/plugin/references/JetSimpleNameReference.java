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
import org.jetbrains.jet.compiler.TipsManager;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.completion.handlers.JetFunctionInsertHandler;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.List;

/**
* @author yole
*/
class JetSimpleNameReference extends JetPsiReference {

    private final static JetFunctionInsertHandler EMPTY_FUNCTION_HANDLER = new JetFunctionInsertHandler(
            JetFunctionInsertHandler.CaretPosition.AFTER_BRACKETS);

    private final static JetFunctionInsertHandler PARAMS_FUNCTION_HANDLER = new JetFunctionInsertHandler(
            JetFunctionInsertHandler.CaretPosition.IN_BRACKETS);

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
        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(
                (JetFile) myExpression.getContainingFile());

        return collectLookupElements(bindingContext, TipsManager.getReferenceVariants(myExpression, bindingContext));
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        PsiElement element = JetPsiFactory.createNameIdentifier(myExpression.getProject(), newElementName);
        return myExpression.getReferencedNameElement().replace(element);
    }

    private static Object[] collectLookupElements(BindingContext bindingContext, Iterable<DeclarationDescriptor> descriptors) {
        List<LookupElement> result = Lists.newArrayList();

        for (final DeclarationDescriptor descriptor : descriptors) {
            LookupElementBuilder element = LookupElementBuilder.create(descriptor, descriptor.getName());
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
                

                // TODO: A special case when it's impossible to resolve type parameters from arguments. Need '<' caret '>'
                // TODO: Support omitting brackets for one argument functions
                if (functionDescriptor.getValueParameters().isEmpty()) {
                    element = element.setInsertHandler(EMPTY_FUNCTION_HANDLER);
                } else {
                    element = element.setInsertHandler(PARAMS_FUNCTION_HANDLER);
                }
            }
            else if (descriptor instanceof VariableDescriptor) {
                JetType outType = ((VariableDescriptor) descriptor).getOutType();
                typeText = DescriptorRenderer.TEXT.renderType(outType);
            }
            else if (descriptor instanceof ClassDescriptor) {
                tailText = " (" + DescriptorUtils.getFQName(descriptor.getContainingDeclaration()) + ")";
                tailTextGrayed = true;
            }
            else {
                typeText = DescriptorRenderer.TEXT.render(descriptor);
            }
            
            element = element.setTailText(tailText, tailTextGrayed).setTypeText(typeText);

            PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor.getOriginal());
            if (declaration != null) {
                element = element.setIcon(declaration.getIcon(Iconable.ICON_FLAG_OPEN | Iconable.ICON_FLAG_VISIBILITY));
            }

            result.add(element);
        }
        return result.toArray();
    }
}
