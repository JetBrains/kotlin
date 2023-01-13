/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.codeInsight.CommentUtilCore;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.builtins.StandardNames;
import org.jetbrains.kotlin.kdoc.psi.api.KDocElement;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.resolve.StatementFilter;
import org.jetbrains.kotlin.resolve.StatementFilterKt;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class KtPsiUtil {
    private KtPsiUtil() {
    }

    public interface KtExpressionWrapper {
        KtExpression getBaseExpression();
    }

    public static <D> void visitChildren(@NotNull KtElement element, @NotNull KtVisitor<Void, D> visitor, D data) {
        PsiElement child = element.getFirstChild();
        while (child != null) {
            if (child instanceof KtElement) {
                ((KtElement) child).accept(visitor, data);
            }
            child = child.getNextSibling();
        }
    }

    @NotNull
    public static KtExpression safeDeparenthesize(@NotNull KtExpression expression) {
        return safeDeparenthesize(expression, false);
    }

    @NotNull
    public static KtExpression safeDeparenthesize(@NotNull KtExpression expression, boolean keepAnnotations) {
        KtExpression deparenthesized = deparenthesize(expression, keepAnnotations);
        return deparenthesized != null ? deparenthesized : expression;
    }

    @Nullable
    public static KtExpression deparenthesize(@Nullable KtExpression expression) {
        return deparenthesize(expression, false);
    }

    @Nullable
    public static KtExpression deparenthesize(@Nullable KtExpression expression, boolean keepAnnotations) {
        while (true) {
            KtExpression baseExpression = deparenthesizeOnce(expression, keepAnnotations);

            if (baseExpression == expression) return baseExpression;
            expression = baseExpression;
        }
    }

    @Nullable
    public static KtExpression deparenthesizeOnce(
            @Nullable KtExpression expression
    ) {
        return deparenthesizeOnce(expression, false);
    }

    @Nullable
    public static KtExpression deparenthesizeOnce(
            @Nullable KtExpression expression, boolean keepAnnotations
    ) {
        if (expression instanceof KtAnnotatedExpression && !keepAnnotations) {
            return ((KtAnnotatedExpression) expression).getBaseExpression();
        }
        else if (expression instanceof KtLabeledExpression) {
            return ((KtLabeledExpression) expression).getBaseExpression();
        }
        else if (expression instanceof KtExpressionWrapper) {
            return ((KtExpressionWrapper) expression).getBaseExpression();
        }
        else if (expression instanceof KtParenthesizedExpression) {
            return ((KtParenthesizedExpression) expression).getExpression();
        }
        return expression;
    }

    @NotNull
    public static Name safeName(@Nullable String name) {
        return name == null ? SpecialNames.NO_NAME_PROVIDED : Name.identifier(name);
    }

    @NotNull
    public static Set<KtElement> findRootExpressions(@NotNull Collection<KtElement> unreachableElements) {
        Set<KtElement> rootElements = new HashSet<>();
        Set<KtElement> shadowedElements = new HashSet<>();
        KtVisitorVoid shadowAllChildren = new KtVisitorVoid() {
            @Override
            public void visitKtElement(@NotNull KtElement element) {
                if (shadowedElements.add(element)) {
                    element.acceptChildren(this);
                }
            }
        };

        for (KtElement element : unreachableElements) {
            if (shadowedElements.contains(element)) continue;
            element.acceptChildren(shadowAllChildren);

            rootElements.removeAll(shadowedElements);
            rootElements.add(element);
        }
        return rootElements;
    }

    @NotNull
    public static String unquoteIdentifier(@NotNull String quoted) {
        if (quoted.indexOf('`') < 0) {
            return quoted;
        }

        if (quoted.startsWith("`") && quoted.endsWith("`") && quoted.length() >= 2) {
            return quoted.substring(1, quoted.length() - 1);
        }
        else {
            return quoted;
        }
    }

    @NotNull
    public static String unquoteIdentifierOrFieldReference(@NotNull String quoted) {
        if (quoted.indexOf('`') < 0) {
            return quoted;
        }

        if (quoted.startsWith("$")) {
            return "$" + unquoteIdentifier(quoted.substring(1));
        }
        else {
            return unquoteIdentifier(quoted);
        }
    }

    public static boolean isDeprecated(@NotNull KtModifierListOwner owner) {
        KtModifierList modifierList = owner.getModifierList();
        if (modifierList != null) {
            List<KtAnnotationEntry> annotationEntries = modifierList.getAnnotationEntries();
            for (KtAnnotationEntry annotation : annotationEntries) {
                Name shortName = annotation.getShortName();
                if (StandardNames.FqNames.deprecated.shortName().equals(shortName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static <T extends PsiElement> T getDirectParentOfTypeForBlock(@NotNull KtBlockExpression block, @NotNull Class<T> aClass) {
        T parent = PsiTreeUtil.getParentOfType(block, aClass);
        if (parent instanceof KtIfExpression) {
            KtIfExpression ifExpression = (KtIfExpression) parent;
            if (ifExpression.getElse() == block || ifExpression.getThen() == block) {
                return parent;
            }
        }
        if (parent instanceof KtWhenExpression) {
            KtWhenExpression whenExpression = (KtWhenExpression) parent;
            for (KtWhenEntry whenEntry : whenExpression.getEntries()) {
                if (whenEntry.getExpression() == block) {
                    return parent;
                }
            }
        }
        if (parent instanceof KtFunctionLiteral) {
            KtFunctionLiteral functionLiteral = (KtFunctionLiteral) parent;
            if (functionLiteral.getBodyExpression() == block) {
                return parent;
            }
        }
        if (parent instanceof KtTryExpression) {
            KtTryExpression tryExpression = (KtTryExpression) parent;
            if (tryExpression.getTryBlock() == block) {
                return parent;
            }
            for (KtCatchClause clause : tryExpression.getCatchClauses()) {
                if (clause.getCatchBody() == block) {
                    return parent;
                }
            }
        }
        return null;
    }

    @Nullable
    public static KtSimpleNameExpression getLastReference(@NotNull KtExpression importedReference) {
        KtElement selector = KtPsiUtilKt.getQualifiedElementSelector(importedReference);
        return selector instanceof KtSimpleNameExpression ? (KtSimpleNameExpression) selector : null;
    }

    public static boolean isSelectorInQualified(@NotNull KtSimpleNameExpression nameExpression) {
        KtElement qualifiedElement = KtPsiUtilKt.getQualifiedElement(nameExpression);
        return qualifiedElement instanceof KtQualifiedExpression
               || ((qualifiedElement instanceof KtUserType) && ((KtUserType) qualifiedElement).getQualifier() != null);
    }

    public static boolean isLHSOfDot(@NotNull KtExpression expression) {
        PsiElement parent = expression.getParent();
        if (!(parent instanceof KtQualifiedExpression)) return false;
        KtQualifiedExpression qualifiedParent = (KtQualifiedExpression) parent;
        return qualifiedParent.getReceiverExpression() == expression || isLHSOfDot(qualifiedParent);
    }

    public static boolean isScriptDeclaration(@NotNull KtDeclaration namedDeclaration) {
        return getScript(namedDeclaration) != null;
    }

    @Nullable
    public static KtScript getScript(@NotNull KtDeclaration namedDeclaration) {
        PsiElement parent = namedDeclaration.getParent();
        if (parent != null && parent.getParent() instanceof KtScript) {
            return (KtScript) parent.getParent();
        }
        else {
            return null;
        }
    }

    public static boolean isRemovableVariableDeclaration(@NotNull KtDeclaration declaration) {
        if (!(declaration instanceof KtVariableDeclaration)) return false;
        if (declaration instanceof KtProperty) return true;
        assert declaration instanceof KtDestructuringDeclarationEntry;
        // We can always replace destructuring entry with _
        return true;
    }

    @Nullable
    @SafeVarargs
    @Contract("null, _ -> null")
    public static PsiElement getTopmostParentOfTypes(
            @Nullable PsiElement element,
            @NotNull Class<? extends PsiElement>... parentTypes
    ) {
        if (element instanceof PsiFile) return null;

        PsiElement answer = PsiTreeUtil.getParentOfType(element, parentTypes);
        if (answer instanceof PsiFile) return answer;

        do {
            PsiElement next = PsiTreeUtil.getParentOfType(answer, parentTypes);
            if (next == null) break;
            answer = next;
        }
        while (true);

        return answer;
    }

    public static boolean isNullConstant(@NotNull KtExpression expression) {
        KtExpression deparenthesized = deparenthesize(expression);
        return deparenthesized instanceof KtConstantExpression && deparenthesized.getNode().getElementType() == KtNodeTypes.NULL;
    }

    public static boolean isTrueConstant(@Nullable KtExpression condition) {
        return isBooleanConstant(condition) && condition.getNode().findChildByType(KtTokens.TRUE_KEYWORD) != null;
    }

    public static boolean isFalseConstant(@Nullable KtExpression condition) {
        return isBooleanConstant(condition) && condition.getNode().findChildByType(KtTokens.FALSE_KEYWORD) != null;
    }

    public static boolean isBooleanConstant(@Nullable KtExpression condition) {
        return condition != null && condition.getNode().getElementType() == KtNodeTypes.BOOLEAN_CONSTANT;
    }

    public static boolean isAbstract(@NotNull KtDeclarationWithBody declaration) {
        return declaration.getBodyExpression() == null;
    }

    @Nullable
    public static KtExpression getExpressionOrLastStatementInBlock(@Nullable KtExpression expression) {
        if (expression instanceof KtBlockExpression) {
            return getLastStatementInABlock((KtBlockExpression) expression);
        }
        return expression;
    }

    @Nullable
    public static KtExpression getLastStatementInABlock(@Nullable KtBlockExpression blockExpression) {
        if (blockExpression == null) return null;
        List<KtExpression> statements = blockExpression.getStatements();
        return statements.isEmpty() ? null : statements.get(statements.size() - 1);
    }

    public static boolean isTrait(@NotNull KtClassOrObject classOrObject) {
        return classOrObject instanceof KtClass && ((KtClass) classOrObject).isInterface();
    }

    @Nullable
    public static KtClassOrObject getOutermostClassOrObject(@NotNull KtClassOrObject classOrObject) {
        KtClassOrObject current = classOrObject;
        while (true) {
            PsiElement parent = current.getParent();
            assert parent != null : "Class with no parent: " + current.getText();

            if (parent instanceof PsiFile) {
                return current;
            }
            if (!(parent instanceof KtClassBody)) {
                // It is a local class, no legitimate outer
                return current;
            }

            current = (KtClassOrObject) parent.getParent();
        }
    }

    @Nullable
    public static KtClassOrObject getClassIfParameterIsProperty(@NotNull KtParameter ktParameter) {
        if (ktParameter.hasValOrVar()) {
            PsiElement grandParent = ktParameter.getParent().getParent();
            if (grandParent instanceof KtPrimaryConstructor) {
                return ((KtPrimaryConstructor) grandParent).getContainingClassOrObject();
            }
        }

        return null;
    }

    @Nullable
    private static IElementType getOperation(@NotNull KtExpression expression) {
        if (expression instanceof KtQualifiedExpression) {
            return ((KtQualifiedExpression) expression).getOperationSign();
        }
        else if (expression instanceof KtOperationExpression) {
            return ((KtOperationExpression) expression).getOperationReference().getReferencedNameElementType();
        }
        return null;
    }


    private static int getPriority(@NotNull KtExpression expression) {
        int maxPriority = KotlinExpressionParsing.Precedence.values().length + 1;

        // same as postfix operations
        if (expression instanceof KtPostfixExpression ||
            expression instanceof KtQualifiedExpression ||
            expression instanceof KtCallExpression ||
            expression instanceof KtArrayAccessExpression ||
            expression instanceof KtDoubleColonExpression) {
            return maxPriority - 1;
        }

        if (expression instanceof KtPrefixExpression || expression instanceof KtLabeledExpression) return maxPriority - 2;

        if (expression instanceof KtIfExpression) {
            return KotlinExpressionParsing.Precedence.ASSIGNMENT.ordinal();
        }

        if (expression instanceof KtSuperExpression) {
            return maxPriority;
        }

        if (expression instanceof KtDeclaration || expression instanceof KtStatementExpression) {
            return 0;
        }

        IElementType operation = getOperation(expression);
        for (KotlinExpressionParsing.Precedence precedence : KotlinExpressionParsing.Precedence.values()) {
            if (precedence != KotlinExpressionParsing.Precedence.PREFIX && precedence != KotlinExpressionParsing.Precedence.POSTFIX &&
                precedence.getOperations().contains(operation)) {
                return maxPriority - precedence.ordinal() - 1;
            }
        }

        return maxPriority;
    }

    @SuppressWarnings("unused") // used in intellij repo
    public static boolean areParenthesesUseless(@NotNull KtParenthesizedExpression expression) {
        KtExpression innerExpression = expression.getExpression();
        if (innerExpression == null) return true;
        PsiElement parent = expression.getParent();
        if (!(parent instanceof KtElement)) return true;
        return !areParenthesesNecessary(innerExpression, expression, (KtElement) parent);
    }

    public static boolean areParenthesesNecessary(
            @NotNull KtExpression innerExpression,
            @NotNull KtExpression currentInner,
            @NotNull KtElement parentElement
    ) {
        if (parentElement instanceof KtDelegatedSuperTypeEntry) return true;

        if (parentElement instanceof KtParenthesizedExpression || innerExpression instanceof KtParenthesizedExpression) {
            return false;
        }

        if (parentElement instanceof KtPackageDirective) return false;

        if (parentElement instanceof KtWhenExpression || innerExpression instanceof KtWhenExpression) {
            return false;
        }

        if (parentElement instanceof KtCollectionLiteralExpression) return false;

        if (innerExpression instanceof KtIfExpression) {
            if (parentElement instanceof KtQualifiedExpression) return true;

            PsiElement current = parentElement;

            while (!(current instanceof KtBlockExpression || current instanceof KtDeclaration || current instanceof KtStatementExpression || current instanceof KtFile)) {
                if (current.getTextRange().getEndOffset() != currentInner.getTextRange().getEndOffset()) {
                    return !(current instanceof KtParenthesizedExpression) && !(current instanceof KtValueArgumentList); // if current expression is "guarded" by parenthesis, no extra parenthesis is necessary
                }

                current = current.getParent();
            }
        }

        if (innerExpression instanceof KtLambdaExpression) {
            PsiElement prevSibling = PsiTreeUtil.skipWhitespacesAndCommentsBackward(currentInner);
            if (endWithParenthesisOrCallExpression(prevSibling)) return true;
        }

        if (parentElement instanceof KtCallExpression && currentInner == ((KtCallExpression) parentElement).getCalleeExpression()) {
            KtCallExpression parentCall = (KtCallExpression) parentElement;
            KtExpression targetInnerExpression = innerExpression;
            if (targetInnerExpression instanceof KtDotQualifiedExpression) {
                KtExpression selector = ((KtDotQualifiedExpression) targetInnerExpression).getSelectorExpression();
                if (selector != null) {
                    targetInnerExpression = selector;
                }
            }
            if (targetInnerExpression instanceof KtSimpleNameExpression) return false;
            if (KtPsiUtilKt.getQualifiedExpressionForSelector(parentElement) != null) return true;
            if (targetInnerExpression instanceof KtCallExpression && parentCall.getValueArgumentList() == null) return true;
            return !(targetInnerExpression instanceof KtThisExpression
                     || targetInnerExpression instanceof KtArrayAccessExpression
                     || targetInnerExpression instanceof KtConstantExpression
                     || targetInnerExpression instanceof KtStringTemplateExpression
                     || targetInnerExpression instanceof KtCallExpression);
        }

        if (parentElement instanceof KtValueArgument) {
            // a(___, d > (e + f)) => a((b < c), d > (e + f)) to prevent parsing < c, d > as type argument list
            KtValueArgument nextArg = PsiTreeUtil.getNextSiblingOfType(parentElement, KtValueArgument.class);
            PsiElement nextExpression = nextArg != null ? nextArg.getArgumentExpression() : null;
            if (innerExpression instanceof KtBinaryExpression &&
                ((KtBinaryExpression) innerExpression).getOperationToken() == KtTokens.LT &&
                nextExpression instanceof KtBinaryExpression &&
                ((KtBinaryExpression) nextExpression).getOperationToken() == KtTokens.GT) return true;
        }

        IElementType innerOperation = getOperation(innerExpression);

        if (innerExpression instanceof KtBinaryExpression) {
            // '(x operator return [...]) operator ...' case
            if (parentElement instanceof KtBinaryExpression) {
                KtBinaryExpression innerBinary = (KtBinaryExpression) innerExpression;
                if (innerBinary.getRight() instanceof KtReturnExpression) {
                    return true;
                }
            }
            // '(x operator y)' case
            if (innerOperation != KtTokens.ELVIS &&
                !(parentElement instanceof KtValueArgument) &&
                !(parentElement instanceof KtParameter) &&
                !(parentElement instanceof KtBlockStringTemplateEntry) &&
                !(parentElement instanceof KtContainerNode &&
                  // for `if` branch, `else` branch, loops body and `when` entry parentheses are required
                  !(parentElement instanceof KtContainerNodeForControlStructureBody)) &&
                isKeepBinaryExpressionParenthesized((KtBinaryExpression) innerExpression)) {
                return true;
            }
        }

        if (!(parentElement instanceof KtExpression)) return false;

        IElementType parentOperation = getOperation((KtExpression) parentElement);

        // 'return (@label{...})' case
        if (parentElement instanceof KtReturnExpression
            && (innerExpression instanceof KtLabeledExpression || innerExpression instanceof KtAnnotatedExpression)) return true;

        // '(x: Int) < y' case
        if (innerExpression instanceof KtBinaryExpressionWithTypeRHS && parentOperation == KtTokens.LT) {
            return true;
        }

        if (parentElement instanceof KtLabeledExpression) return false;

        // 'x ?: ...' case
        if (parentElement instanceof KtBinaryExpression &&
            parentOperation == KtTokens.ELVIS &&
            !(innerExpression instanceof KtBinaryExpression) &&
            currentInner == ((KtBinaryExpression) parentElement).getRight()) {
            return false;
        }

        // 'x = fun {}' case
        if (parentElement instanceof KtBinaryExpression &&
            parentOperation == KtTokens.EQ &&
            innerExpression instanceof KtNamedFunction &&
            currentInner == ((KtBinaryExpression) parentElement).getRight()) {
            return false;
        }

        int innerPriority = getPriority(innerExpression);
        int parentPriority = getPriority((KtExpression) parentElement);

        if (innerPriority == parentPriority) {
            if (parentElement instanceof KtBinaryExpression) {
                if (innerOperation == KtTokens.ANDAND || innerOperation == KtTokens.OROR) {
                    return false;
                }
                return ((KtBinaryExpression) parentElement).getRight() == currentInner;
            }

            if (parentElement instanceof KtPrefixExpression && innerExpression instanceof KtPrefixExpression) {
                // +(++x) or +(+x) case
                if (parentOperation == KtTokens.PLUS) {
                    return innerOperation == KtTokens.PLUS || innerOperation == KtTokens.PLUSPLUS;
                }

                // -(--x) or -(-x) case
                if (parentOperation == KtTokens.MINUS) {
                    return innerOperation == KtTokens.MINUS || innerOperation == KtTokens.MINUSMINUS;
                }
            }
            return false;
        }

        return innerPriority < parentPriority;
    }

    private static boolean endWithParenthesisOrCallExpression(PsiElement element) {
        if (element == null) return false;
        if (element.getText().endsWith(KtTokens.RPAR.getValue()) || element instanceof KtCallExpression) return true;
        PsiElement[] children = element.getChildren();
        int length = children.length;
        if (length == 0) return false;
        return endWithParenthesisOrCallExpression(children[length - 1]);
    }

    private static boolean isKeepBinaryExpressionParenthesized(KtBinaryExpression expression) {
        PsiElement expr = expression.getFirstChild();
        while (expr != null) {
            if (expr instanceof PsiWhiteSpace && expr.textContains('\n')) {
                return true;
            }
            if (expr instanceof KtOperationReferenceExpression) {
                break;
            }
            expr = expr.getNextSibling();
        }
        return (expression.getRight() instanceof KtBinaryExpression && isKeepBinaryExpressionParenthesized((KtBinaryExpression) expression.getRight())) ||
               (expression.getLeft() instanceof KtBinaryExpression && isKeepBinaryExpressionParenthesized((KtBinaryExpression) expression.getLeft()));
    }

    public static boolean isAssignment(@NotNull PsiElement element) {
        return element instanceof KtBinaryExpression &&
               KtTokens.ALL_ASSIGNMENTS.contains(((KtBinaryExpression) element).getOperationToken());
    }

    public static boolean isOrdinaryAssignment(@NotNull PsiElement element) {
        return element instanceof KtBinaryExpression &&
               ((KtBinaryExpression) element).getOperationToken().equals(KtTokens.EQ);
    }

    public static boolean isCast(@NotNull KtBinaryExpressionWithTypeRHS expression) {
        return isSafeCast(expression) || isUnsafeCast(expression);
    }

    public static boolean isSafeCast(@NotNull KtBinaryExpressionWithTypeRHS expression) {
        return expression.getOperationReference().getReferencedNameElementType() == KtTokens.AS_SAFE;
    }

    public static boolean isUnsafeCast(@NotNull KtBinaryExpressionWithTypeRHS expression) {
        return expression.getOperationReference().getReferencedNameElementType() == KtTokens.AS_KEYWORD;
    }

    public static boolean checkVariableDeclarationInBlock(@NotNull KtBlockExpression block, @NotNull String varName) {
        for (KtExpression element : block.getStatements()) {
            if (element instanceof KtVariableDeclaration) {
                if (((KtVariableDeclaration) element).getNameAsSafeName().asString().equals(varName)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean checkWhenExpressionHasSingleElse(@NotNull KtWhenExpression whenExpression) {
        int elseCount = 0;
        for (KtWhenEntry entry : whenExpression.getEntries()) {
            if (entry.isElse()) {
                elseCount++;
            }
        }
        return (elseCount == 1);
    }

    @Nullable
    public static PsiElement skipTrailingWhitespacesAndComments(@Nullable PsiElement element)  {
        return PsiTreeUtil.skipSiblingsForward(element, PsiWhiteSpace.class, PsiComment.class);
    }

    @Nullable
    public static PsiElement prevLeafIgnoringWhitespaceAndComments(@NotNull PsiElement element) {
        PsiElement prev = PsiTreeUtil.prevLeaf(element, true);
        while (prev != null && KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(prev.getNode().getElementType())) {
            prev = PsiTreeUtil.prevLeaf(prev, true);
        }
        return prev;
    }

    @Nullable
    public static PsiElement getPreviousWord(@NotNull PsiElement element, @NotNull String word) {
        PsiElement prev = prevLeafIgnoringWhitespaceAndComments(element);
        if (prev != null && prev.getNode().getElementType() == KtTokens.IDENTIFIER && word.equals(prev.getText())) {
            return prev;
        }

        return null;
    }

    @NotNull
    public static String getText(@Nullable PsiElement element) {
        return element != null ? element.getText() : "";
    }

    @Nullable
    public static String getNullableText(@Nullable PsiElement element) {
        return element != null ? element.getText() : null;
    }

    /**
     * CommentUtilCore.isComment fails if element <strong>inside</strong> comment.
     *
     * Also, we can not add KDocTokens to COMMENTS TokenSet, because it is used in KotlinParserDefinition.getCommentTokens(),
     * and therefor all COMMENTS tokens will be ignored by PsiBuilder.
     *
     * @param element
     * @return
     */
    public static boolean isInComment(PsiElement element) {
        return CommentUtilCore.isComment(element) || element instanceof KDocElement;
    }

    @Nullable
    public static PsiElement getOutermostParent(@NotNull PsiElement element, @NotNull PsiElement upperBound, boolean strict) {
        PsiElement parent = strict ? element.getParent() : element;
        while (parent != null && parent.getParent() != upperBound) {
            parent = parent.getParent();
        }

        return parent;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getLastChildByType(@NotNull PsiElement root, @NotNull Class<? extends T>... elementTypes) {
        PsiElement[] children = root.getChildren();

        for (int i = children.length - 1; i >= 0; i--) {
            if (PsiTreeUtil.instanceOf(children[i], elementTypes)) {
                return (T) children[i];
            }
        }

        return null;
    }

    @Nullable
    public static KtElement getOutermostDescendantElement(
            @Nullable PsiElement root,
            boolean first,
            @NotNull Predicate<KtElement> predicate
    ) {
        if (!(root instanceof KtElement)) return null;

        List<KtElement> results = Lists.newArrayList();

        root.accept(
                new KtVisitorVoid() {
                    @Override
                    public void visitKtElement(@NotNull KtElement element) {
                        if (predicate.test(element)) {
                            //noinspection unchecked
                            results.add(element);
                        }
                        else {
                            element.acceptChildren(this);
                        }
                    }
                }
        );

        if (results.isEmpty()) return null;

        return first ? results.get(0) : results.get(results.size() - 1);
    }

    @Nullable
    public static PsiElement findChildByType(@NotNull PsiElement element, @NotNull IElementType type) {
        ASTNode node = element.getNode().findChildByType(type);
        return node == null ? null : node.getPsi();
    }

    @Nullable
    public static PsiElement skipSiblingsBackwardByPredicate(@Nullable PsiElement element, Predicate<PsiElement> elementsToSkip) {
        if (element == null) return null;
        for (PsiElement e = element.getPrevSibling(); e != null; e = e.getPrevSibling()) {
            if (elementsToSkip.test(e)) continue;
            return e;
        }
        return null;
    }

    public static PsiElement ascendIfPropertyAccessor(PsiElement element) {
        if (element instanceof KtPropertyAccessor) {
            return element.getParent();
        }
        return element;
    }

    @Nullable
    @Contract("_, !null -> !null")
    public static KtModifierList replaceModifierList(@NotNull KtModifierListOwner owner, @Nullable KtModifierList modifierList) {
        KtModifierList oldModifierList = owner.getModifierList();
        if (modifierList == null) {
            if (oldModifierList != null) oldModifierList.delete();
            return null;
        }
        else {
            if (oldModifierList == null) {
                PsiElement firstChild = owner.getFirstChild();
                return (KtModifierList) owner.addBefore(modifierList, firstChild);
            }
            else {
                return (KtModifierList) oldModifierList.replace(modifierList);
            }
        }
    }

    @Nullable
    public static String getPackageName(@NotNull KtElement element) {
        KtFile file = element.getContainingKtFile();
        KtPackageDirective header = PsiTreeUtil.findChildOfType(file, KtPackageDirective.class);

        return header != null ? header.getQualifiedName() : null;
    }

    @Nullable
    public static KtElement getEnclosingElementForLocalDeclaration(@NotNull KtDeclaration declaration) {
        return getEnclosingElementForLocalDeclaration(declaration, true);
    }

    private static boolean isMemberOfObjectExpression(@NotNull KtCallableDeclaration propertyOrFunction) {
        PsiElement parent = PsiTreeUtil.getStubOrPsiParent(propertyOrFunction);
        if (!(parent instanceof KtClassBody)) return false;
        PsiElement grandparent = PsiTreeUtil.getStubOrPsiParent(parent);
        if (!(grandparent instanceof KtObjectDeclaration)) return false;
        return PsiTreeUtil.getStubOrPsiParent(grandparent) instanceof KtObjectLiteralExpression;
    }

    private static boolean isNonLocalCallable(@Nullable KtDeclaration declaration) {
        if (declaration instanceof KtProperty) {
            return !((KtProperty) declaration).isLocal();
        }
        else if (declaration instanceof KtFunction) {
            return !((KtFunction) declaration).isLocal();
        }
        return false;
    }

    @Nullable
    public static KtElement getEnclosingElementForLocalDeclaration(@NotNull KtDeclaration declaration, boolean skipParameters) {
        if (declaration instanceof KtTypeParameter && skipParameters) {
            declaration = PsiTreeUtil.getParentOfType(declaration, KtNamedDeclaration.class);
        }
        else if (declaration instanceof KtParameter) {
            KtFunctionType functionType = PsiTreeUtil.getParentOfType(declaration, KtFunctionType.class);
            if (functionType != null) {
                return functionType;
            }

            PsiElement parent = declaration.getParent();

            // val/var parameter of primary constructor should be considered as local according to containing class
            if (((KtParameter) declaration).hasValOrVar() && parent != null && parent.getParent() instanceof KtPrimaryConstructor) {
                return getEnclosingElementForLocalDeclaration(((KtPrimaryConstructor) parent.getParent()).getContainingClassOrObject(), skipParameters);
            }
            else if (skipParameters && parent != null &&
                     !(parent instanceof KtForExpression) &&
                     parent.getParent() instanceof KtNamedFunction) {
                declaration = (KtNamedFunction) parent.getParent();
            }
        }

        if (declaration instanceof PsiFile) {
            return declaration;
        }

        // No appropriate stub-tolerant method in PsiTreeUtil, nor JetStubbedPsiUtil, writing manually
        PsiElement current = PsiTreeUtil.getStubOrPsiParent(declaration);
        boolean isNonLocalCallable = isNonLocalCallable(declaration);
        while (current != null) {
            PsiElement parent = PsiTreeUtil.getStubOrPsiParent(current);
            if (parent instanceof KtScript) return null;
            if (current instanceof KtAnonymousInitializer) {
                return ((KtAnonymousInitializer) current).getBody();
            }
            if (current instanceof KtProperty || current instanceof KtFunction) {
                if (parent instanceof KtFile) {
                    return (KtElement) current;
                }
                else if (parent instanceof KtClassBody && !isMemberOfObjectExpression((KtCallableDeclaration) current)) {
                    return (KtElement) parent;
                }
                else if (parent instanceof KtBlockExpression) {
                    PsiElement grandParent = parent.getParent();
                    if (grandParent instanceof KtScript) {
                        return (KtElement) parent;
                    }
                }
            }
            if (current instanceof KtParameter) {
                return (KtElement) current;
            }
            if (current instanceof KtValueArgument) {
                // for members, value argument is never enough, see KT-10546
                if (!isNonLocalCallable) {
                    return (KtElement) current;
                }
            }
            if (current instanceof KtBlockExpression) {
                // For members also not applicable if has function literal parent
                if (!isNonLocalCallable || !(current.getParent() instanceof KtFunctionLiteral)) {
                    return (KtElement) current;
                }
            }
            if (current instanceof KtDelegatedSuperTypeEntry || current instanceof KtSuperTypeCallEntry) {
                PsiElement grandParent = current.getParent().getParent();
                if (grandParent instanceof KtClassOrObject && !(grandParent.getParent() instanceof KtObjectLiteralExpression)) {
                    return (KtElement) grandParent;
                }
            }

            current = parent;
        }
        return null;
    }

    public static boolean isLocal(@NotNull KtDeclaration declaration) {
        return getEnclosingElementForLocalDeclaration(declaration) != null;
    }

    @Nullable
    public static KtToken getOperationToken(@NotNull KtOperationExpression expression) {
        KtSimpleNameExpression operationExpression = expression.getOperationReference();
        IElementType elementType = operationExpression.getReferencedNameElementType();
        assert elementType == null || elementType instanceof KtToken :
                "KtOperationExpression should have operation token of type KtToken: " +
                expression;
        return (KtToken) elementType;
    }

    public static boolean isLabelIdentifierExpression(PsiElement element) {
        return element instanceof KtLabelReferenceExpression;
    }

    @Nullable
    public static KtExpression getParentCallIfPresent(@NotNull KtExpression expression) {
        PsiElement parent = expression.getParent();
        while (parent != null) {
            if (parent instanceof KtBinaryExpression ||
                parent instanceof KtUnaryExpression ||
                parent instanceof KtLabeledExpression ||
                parent instanceof KtDotQualifiedExpression ||
                parent instanceof KtCallExpression ||
                parent instanceof KtArrayAccessExpression ||
                parent instanceof KtDestructuringDeclaration) {

                if (parent instanceof KtLabeledExpression) {
                    parent = parent.getParent();
                    continue;
                }

                //check that it's in inlineable call would be in resolve call of parent
                return (KtExpression) parent;
            }
            else if (parent instanceof KtParenthesizedExpression || parent instanceof KtBinaryExpressionWithTypeRHS) {
                parent = parent.getParent();
            }
            else if (parent instanceof KtValueArgument || parent instanceof KtValueArgumentList) {
                parent = parent.getParent();
            }
            else if (parent instanceof KtLambdaExpression || parent instanceof KtAnnotatedExpression) {
                parent = parent.getParent();
            }
            else {
                return null;
            }
        }
        return null;
    }

    public static boolean isLabeledFunctionLiteral(@NotNull KtFunctionLiteral functionLiteral) {
        // KtFunctionLiteral -> KtLambdaExpression -> KtLabeledExpression
        return functionLiteral.getParent().getParent() instanceof KtLabeledExpression;
    }

    @Nullable
    public static KtExpression getLastElementDeparenthesized(
            @Nullable KtExpression expression,
            @NotNull StatementFilter statementFilter
    ) {
        KtExpression deparenthesizedExpression = deparenthesize(expression);
        if (deparenthesizedExpression instanceof KtBlockExpression) {
            KtBlockExpression blockExpression = (KtBlockExpression) deparenthesizedExpression;
            // todo
            // This case is a temporary hack for 'if' branches.
            // The right way to implement this logic is to interpret 'if' branches as function literals with explicitly-typed signatures
            // (no arguments and no receiver) and therefore analyze them straight away (not in the 'complete' phase).
            KtExpression lastStatementInABlock = StatementFilterKt.getLastStatementInABlock(statementFilter, blockExpression);
            if (lastStatementInABlock != null) {
                return getLastElementDeparenthesized(lastStatementInABlock, statementFilter);
            }
        }
        return deparenthesizedExpression;
    }

    public static boolean isStatementContainer(@Nullable PsiElement container) {
        return container instanceof KtBlockExpression ||
               container instanceof KtContainerNodeForControlStructureBody ||
               container instanceof KtWhenEntry;
    }

    public static boolean isStatement(@NotNull PsiElement element) {
        return isStatementContainer(element.getParent());
    }

    public static boolean isInOrNotInOperation(@NotNull KtBinaryExpression binaryExpression) {
        return isInOperation(binaryExpression) || isNotInOperation(binaryExpression);
    }

    public static boolean isNotInOperation(@NotNull KtBinaryExpression binaryExpression) {
        return (binaryExpression.getOperationToken() == KtTokens.NOT_IN);
    }

    private static boolean isInOperation(@NotNull KtBinaryExpression binaryExpression) {
        return (binaryExpression.getOperationToken() == KtTokens.IN_KEYWORD);
    }
}
