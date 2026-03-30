/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.KotlinValueArgumentStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a value argument in a function call.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * println("Hello")
 * //      ^_____^
 * }</pre>
 */
public class KtValueArgument extends KtElementImplStub<KotlinValueArgumentStub<? extends KtValueArgument>> implements ValueArgument {
    private static final Set<String> PACK_SELECTOR_NAMES = Collections.unmodifiableSet(new LinkedHashSet<>(
            Arrays.asList("$props", "$sharedProps", "$attrs", "$callbacks", "$slots")
    ));

    public KtValueArgument(@NotNull ASTNode node) {
        super(node);
    }

    public KtValueArgument(@NotNull KotlinValueArgumentStub<KtValueArgument> stub) {
        super(stub, KtStubBasedElementTypes.VALUE_ARGUMENT);
    }

    protected KtValueArgument(KotlinValueArgumentStub<? extends KtValueArgument> stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitArgument(this, data);
    }

    private static final TokenSet STRING_TEMPLATE_EXPRESSIONS_TYPES = TokenSet.create(
            KtStubBasedElementTypes.STRING_TEMPLATE
    );

    @Override
    @Nullable
    @IfNotParsed
    public KtExpression getArgumentExpression() {
        KotlinPlaceHolderStub<? extends KtValueArgument> stub = getStub();
        if (stub != null) {
            KtExpression[] constantExpressions =
                    stub.getChildrenByType(KtTokenSets.CONSTANT_EXPRESSIONS, KtExpression.EMPTY_ARRAY);
            if (constantExpressions.length != 0) {
                return constantExpressions[0];
            }
        }

        return findChildByClass(KtExpression.class);
    }

    @Nullable
    public KtStringTemplateExpression getStringTemplateExpression() {
        KotlinPlaceHolderStub<? extends KtValueArgument> stub = getStub();
        KtExpression expression;
        if (stub != null) {
            KtExpression[] stringTemplateExpressions = stub.getChildrenByType(STRING_TEMPLATE_EXPRESSIONS_TYPES, KtExpression.EMPTY_ARRAY);
            expression = stringTemplateExpressions.length != 0 ? stringTemplateExpressions[0] : null;
        }
        else {
            expression = findChildByClass(KtExpression.class);
        }
        return expression instanceof KtStringTemplateExpression ? (KtStringTemplateExpression) expression : null;
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtValueArgumentName getArgumentName() {
        return getStubOrPsiChild(KtStubBasedElementTypes.VALUE_ARGUMENT_NAME);
    }

    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(KtTokens.EQ);
    }

    @Override
    public boolean isNamed() {
        return getArgumentName() != null;
    }

    @NotNull
    @Override
    public KtElement asElement() {
        return this;
    }

    @Override
    public LeafPsiElement getSpreadElement() {
        KotlinValueArgumentStub stub = getStub();
        if (stub != null && !stub.isSpread()) {
            return null;
        }

        ASTNode node = getNode().findChildByType(KtTokens.MUL);
        return node == null ? null : (LeafPsiElement) node.getPsi();
    }

    @Nullable
    public LeafPsiElement getParameterSpreadElement() {
        ASTNode node = getNode().findChildByType(KtTokens.RESERVED);
        return node == null ? null : (LeafPsiElement) node.getPsi();
    }

    public boolean isParameterSpread() {
        KotlinValueArgumentStub<?> stub = getGreenStub();
        if (stub != null) {
            return stub.isParameterSpread();
        }

        TextRange textRange = getTextRange();
        PsiFile containingFile = getContainingFile();
        if (textRange == null || containingFile == null) {
            return getParameterSpreadElement() != null;
        }

        CharSequence fileContents = containingFile.getViewProvider().getContents();
        int startOffset = Math.max(0, textRange.getStartOffset());
        int endOffset = Math.min(textRange.getEndOffset(), fileContents.length());
        while (startOffset < endOffset && Character.isWhitespace(fileContents.charAt(startOffset))) {
            startOffset++;
        }

        return startOffset + 2 < endOffset
                && fileContents.charAt(startOffset) == '.'
                && fileContents.charAt(startOffset + 1) == '.'
                && fileContents.charAt(startOffset + 2) == '.';
    }

    @Nullable
    public KtExpression getParameterSpreadReceiverExpression() {
        ParameterSpreadBinding binding = getParameterSpreadBinding();
        return binding != null ? binding.receiverExpression : getArgumentExpression();
    }

    @NotNull
    public Set<Name> getParameterSpreadExcludedNames() {
        ParameterSpreadBinding binding = getParameterSpreadBinding();
        return binding != null ? binding.excludedNames : Collections.emptySet();
    }

    @Override
    public boolean isSpread() {
        KotlinValueArgumentStub stub = getGreenStub();
        if (stub != null) {
            return stub.isSpread();
        }

        return getSpreadElement() != null;
    }

    @Override
    public boolean isExternal() {
        return false;
    }

    @Nullable
    private ParameterSpreadBinding getParameterSpreadBinding() {
        KtExpression argumentExpression = KtPsiUtil.deparenthesize(getArgumentExpression());
        if (argumentExpression == null) {
            return null;
        }

        Set<Name> excludedNames = Collections.emptySet();
        KtExpression selectorExpression = argumentExpression;

        if (selectorExpression instanceof KtDotQualifiedExpression) {
            KtExpression excludeReceiver = ((KtDotQualifiedExpression) selectorExpression).getReceiverExpression();
            KtExpression excludeSelector = ((KtDotQualifiedExpression) selectorExpression).getSelectorExpression();
            if (isExcludeCall(excludeSelector)) {
                excludedNames = extractExcludedNames((KtCallExpression) excludeSelector);
                selectorExpression = KtPsiUtil.deparenthesize(excludeReceiver);
            }
        }

        if (!(selectorExpression instanceof KtDotQualifiedExpression)) {
            return null;
        }

        KtExpression selectorReceiver = ((KtDotQualifiedExpression) selectorExpression).getReceiverExpression();
        KtExpression selectorCallExpression = ((KtDotQualifiedExpression) selectorExpression).getSelectorExpression();
        if (!(selectorCallExpression instanceof KtCallExpression)) {
            return null;
        }

        KtExpression calleeExpression = KtPsiUtil.deparenthesize(((KtCallExpression) selectorCallExpression).getCalleeExpression());
        if (!(calleeExpression instanceof KtSimpleNameExpression)) {
            return null;
        }

        String selectorName = ((KtSimpleNameExpression) calleeExpression).getReferencedName();
        if (!PACK_SELECTOR_NAMES.contains(selectorName)) {
            return null;
        }

        List<? extends ValueArgument> selectorArguments = ((KtCallExpression) selectorCallExpression).getValueArguments();
        if (selectorArguments.size() != 1) {
            return null;
        }

        KtExpression receiverExpression = KtPsiUtil.deparenthesize(selectorArguments.get(0).getArgumentExpression());
        if (receiverExpression == null) {
            return null;
        }

        if (selectorReceiver == null) {
            return null;
        }

        return new ParameterSpreadBinding(receiverExpression, excludedNames);
    }

    private static boolean isExcludeCall(@Nullable KtExpression expression) {
        KtExpression deparenthesized = KtPsiUtil.deparenthesize(expression);
        if (!(deparenthesized instanceof KtCallExpression)) {
            return false;
        }

        KtExpression callee = KtPsiUtil.deparenthesize(((KtCallExpression) deparenthesized).getCalleeExpression());
        return callee instanceof KtSimpleNameExpression
                && "exclude".equals(((KtSimpleNameExpression) callee).getReferencedName());
    }

    @NotNull
    private static Set<Name> extractExcludedNames(@NotNull KtCallExpression excludeCall) {
        List<? extends ValueArgument> excludeArguments = excludeCall.getValueArguments();
        if (excludeArguments.isEmpty()) {
            return Collections.emptySet();
        }

        LinkedHashSet<Name> names = new LinkedHashSet<>();
        for (ValueArgument excludeArgument : excludeArguments) {
            KtExpression excludeExpression = KtPsiUtil.deparenthesize(excludeArgument.getArgumentExpression());
            if (excludeExpression instanceof KtSimpleNameExpression) {
                names.add(((KtSimpleNameExpression) excludeExpression).getReferencedNameAsName());
            }
        }
        return names.isEmpty() ? Collections.emptySet() : names;
    }

    private static final class ParameterSpreadBinding {
        private final KtExpression receiverExpression;
        private final Set<Name> excludedNames;

        private ParameterSpreadBinding(@NotNull KtExpression receiverExpression, @NotNull Set<Name> excludedNames) {
            this.receiverExpression = receiverExpression;
            this.excludedNames = excludedNames;
        }
    }
}
