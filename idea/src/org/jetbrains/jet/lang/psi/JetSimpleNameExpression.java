package org.jetbrains.jet.lang.psi;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.parsing.JetExpressionParsing;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.List;

/**
 * @author max
 */
public class JetSimpleNameExpression extends JetReferenceExpression {
    public static final TokenSet REFERENCE_TOKENS = TokenSet.orSet(JetTokens.LABELS, TokenSet.create(JetTokens.IDENTIFIER, JetTokens.FIELD_IDENTIFIER, JetTokens.THIS_KEYWORD));

    public JetSimpleNameExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable @IfNotParsed
    public String getReferencedName() {
        PsiElement referencedNameElement = getReferencedNameElement();
        if (referencedNameElement == null) {
            return null;
        }
        final String text = referencedNameElement.getNode().getText();
        if (text.startsWith("`") && text.endsWith("`") && text.length() >= 2) {
            return text.substring(1, text.length()-1);
        }
        return text;
    }

    @Nullable @IfNotParsed
    public PsiElement getReferencedNameElement() {
        PsiElement element = findChildByType(REFERENCE_TOKENS);
        if (element == null) {
            element = findChildByType(JetExpressionParsing.ALL_OPERATIONS);
        }
        return element;
    }

    @Nullable @IfNotParsed
    public IElementType getReferencedNameElementType() {
        PsiElement element = getReferencedNameElement();
        return element == null ? null : element.getNode().getElementType();
    }

    @Override
    public PsiReference findReferenceAt(int offset) {
        return getReference();
    }

    @Override
    public PsiReference getReference() {
        return new JetPsiReference() {

            @Override
            public PsiElement getElement() {
                return getReferencedNameElement();
            }

            @Override
            public TextRange getRangeInElement() {
                return new TextRange(0, getElement().getTextLength());
            }

            @NotNull
            @Override
            public Object[] getVariants() {
                PsiElement parent = getParent();
                if (parent instanceof JetQualifiedExpression) {
                    JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) parent;
                    JetExpression receiverExpression = qualifiedExpression.getReceiverExpression();
                    JetFile file = (JetFile) getContainingFile();
                    BindingContext bindingContext = AnalyzingUtils.analyzeFileWithCache(file);
                    final JetType expressionType = bindingContext.getExpressionType(receiverExpression);
                    if (expressionType != null) {
                        return collectLookupElements(bindingContext, expressionType.getMemberScope());
                    }
                }
                else {
                    JetFile file = (JetFile) getContainingFile();
                    BindingContext bindingContext = AnalyzingUtils.analyzeFileWithCache(file);
                    JetScope resolutionScope = bindingContext.getResolutionScope(JetSimpleNameExpression.this);
                    if (resolutionScope != null) {
                        return collectLookupElements(bindingContext, resolutionScope);
                    }
                }

                return EMPTY_ARRAY;
            }

            @Override
            public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
                PsiElement element = JetChangeUtil.createNameIdentifier(getProject(), newElementName);
                return getReferencedNameElement().replace(element);
            }
        };
    }

    private Object[] collectLookupElements(BindingContext bindingContext, JetScope scope) {
        List<LookupElement> result = Lists.newArrayList();
        for (final DeclarationDescriptor descriptor : scope.getAllDescriptors()) {
            PsiElement declaration = bindingContext.getDeclarationPsiElement(descriptor.getOriginal());
            LookupElementBuilder element = LookupElementBuilder.create(descriptor.getName());
            String typeText = "";
            String tailText = "";
            boolean tailTextGrayed = false;
            if (descriptor instanceof FunctionDescriptor) {
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
                JetType returnType = functionDescriptor.getUnsubstitutedReturnType();
                typeText = DescriptorRenderer.TEXT.renderType(returnType);
                tailText = "(" + StringUtil.join(functionDescriptor.getUnsubstitutedValueParameters(), new Function<ValueParameterDescriptor, String>() {
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
                element = element.setIcon(declaration.getIcon(ICON_FLAG_OPEN | ICON_FLAG_VISIBILITY));
            }
            result.add(element);
        }
        return result.toArray();
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitSimpleNameExpression(this);
    }

}
