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
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.builtins.StandardNames;
import org.jetbrains.kotlin.kdoc.psi.api.KDocElement;
import org.jetbrains.kotlin.lang.BinaryOperationPrecedence;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.resolve.StatementFilter;
import org.jetbrains.kotlin.resolve.StatementFilterKt;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A collection of static helper methods for working with the Kotlin PSI.
 *
 * <p>The utilities here cover common low-level operations such as unwrapping parenthesized expressions
 * ({@link #deparenthesize}), inspecting names and identifiers, and testing structural properties of elements
 * (for example, whether a declaration is local). This class is not instantiable.
 */
public class KtPsiUtil {
    private KtPsiUtil() {
    }

    /**
     * An element that wraps a base expression, such as a parenthesized or annotated expression.
     */
    public interface KtExpressionWrapper {
        /**
         * Returns the wrapped base expression.
         */
        KtExpression getBaseExpression();
    }

    /** Dispatches each direct {@link KtElement} child of the given {@code element} to the {@code visitor}. */
    public static <D> void visitChildren(@NotNull KtElement element, @NotNull KtVisitor<Void, D> visitor, D data) {
        PsiElement child = element.getFirstChild();
        while (child != null) {
            if (child instanceof KtElement) {
                ((KtElement) child).accept(visitor, data);
            }
            child = child.getNextSibling();
        }
    }

    /**
     * Like {@link #deparenthesize(KtExpression)}, but returns {@code expression} itself if there is nothing to unwrap.
     */
    @NotNull
    public static KtExpression safeDeparenthesize(@NotNull KtExpression expression) {
        return safeDeparenthesize(expression, false);
    }

    /**
     * Like {@link #deparenthesize(KtExpression, boolean)}, but returns {@code expression} itself if there is nothing to
     * unwrap.
     */
    @NotNull
    public static KtExpression safeDeparenthesize(@NotNull KtExpression expression, boolean keepAnnotations) {
        KtExpression deparenthesized = deparenthesize(expression, keepAnnotations);
        return deparenthesized != null ? deparenthesized : expression;
    }

    /** Recursively unwraps parentheses, labels, and annotations from the given {@code expression}. */
    @Nullable
    public static KtExpression deparenthesize(@Nullable KtExpression expression) {
        return deparenthesize(expression, false);
    }

    /**
     * Recursively unwraps parentheses and labels (and annotations unless {@code keepAnnotations} is {@code true}) from
     * the given {@code expression}, returning the innermost wrapped expression.
     */
    @Nullable
    public static KtExpression deparenthesize(@Nullable KtExpression expression, boolean keepAnnotations) {
        while (true) {
            KtExpression baseExpression = deparenthesizeOnce(expression, keepAnnotations);

            if (baseExpression == expression) return baseExpression;
            expression = baseExpression;
        }
    }

    /** Unwraps a single layer of parentheses, label, or annotation from the given {@code expression}. */
    @Nullable
    public static KtExpression deparenthesizeOnce(
            @Nullable KtExpression expression
    ) {
        return deparenthesizeOnce(expression, false);
    }

    /**
     * Unwraps a single layer of parentheses or label (and annotation unless {@code keepAnnotations} is {@code true})
     * from the given {@code expression}.
     */
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

    /** Returns {@code name} as a {@link Name}, or the special "no name provided" name if {@code name} is {@code null}. */
    @NotNull
    public static Name safeName(@Nullable String name) {
        return name == null ? SpecialNames.NO_NAME_PROVIDED : Name.identifier(name);
    }

    /** Returns the subset of {@code unreachableElements} that are not contained within any other element of the set. */
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

    /**
     * Removes surrounding backticks from a backtick-quoted identifier, returning {@code quoted} unchanged if it has
     * none.
     */
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

    /** Like {@link #unquoteIdentifier}, but also handles a leading {@code $} field-reference prefix. */
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

    /** Returns {@code true} if {@code owner} is syntactically annotated with {@code @Deprecated} (by short name only). */
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

    /**
     * Returns the enclosing element of type {@code aClass} for which {@code block} is a direct body (an {@code if}
     * branch, a {@code when} entry, a lambda body, or a {@code try}/{@code catch} body), or {@code null} otherwise.
     */
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

    /**
     * Returns the last (rightmost) simple-name reference of the given qualified {@code importedReference}, or
     * {@code null}.
     */
    @Nullable
    public static KtSimpleNameExpression getLastReference(@NotNull KtExpression importedReference) {
        KtElement selector = KtPsiUtilKt.getQualifiedElementSelector(importedReference);
        return selector instanceof KtSimpleNameExpression ? (KtSimpleNameExpression) selector : null;
    }

    /**
     * Returns {@code true} if {@code nameExpression} is the selector of a qualified expression or qualified user type.
     */
    public static boolean isSelectorInQualified(@NotNull KtSimpleNameExpression nameExpression) {
        KtElement qualifiedElement = KtPsiUtilKt.getQualifiedElement(nameExpression);
        return qualifiedElement instanceof KtQualifiedExpression
               || ((qualifiedElement instanceof KtUserType) && ((KtUserType) qualifiedElement).getQualifier() != null);
    }

    /**
     * Returns {@code true} if {@code expression} is on the left-hand side (the receiver) of a qualified
     * {@code .}/{@code ?.} expression.
     */
    public static boolean isLHSOfDot(@NotNull KtExpression expression) {
        PsiElement parent = expression.getParent();
        if (!(parent instanceof KtQualifiedExpression)) return false;
        KtQualifiedExpression qualifiedParent = (KtQualifiedExpression) parent;
        return qualifiedParent.getReceiverExpression() == expression || isLHSOfDot(qualifiedParent);
    }

    /** Returns {@code true} if {@code namedDeclaration} is a top-level declaration of a script. */
    public static boolean isScriptDeclaration(@NotNull KtDeclaration namedDeclaration) {
        return getScript(namedDeclaration) != null;
    }

    /** Returns the script this declaration is a top-level member of, or {@code null} if it is not in a script. */
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

    /**
     * Returns {@code true} if {@code declaration} is a variable declaration that can be safely removed or replaced with
     * {@code _}.
     */
    public static boolean isRemovableVariableDeclaration(@NotNull KtDeclaration declaration) {
        if (!(declaration instanceof KtVariableDeclaration)) return false;
        if (declaration instanceof KtProperty) return true;
        assert declaration instanceof KtDestructuringDeclarationEntry;
        // We can always replace destructuring entry with _
        return true;
    }

    /**
     * Returns the topmost ancestor of {@code element} (or the enclosing file) whose type is one of
     * {@code parentTypes}, or {@code null} if there is none.
     */
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

    /** Returns {@code true} if {@code expression} (after deparenthesizing) is the {@code null} literal. */
    public static boolean isNullConstant(@NotNull KtExpression expression) {
        KtExpression deparenthesized = deparenthesize(expression);
        return deparenthesized instanceof KtConstantExpression && deparenthesized.getNode().getElementType() == KtNodeTypes.NULL;
    }

    /** Returns {@code true} if {@code condition} is the boolean literal {@code true}. */
    public static boolean isTrueConstant(@Nullable KtExpression condition) {
        return isBooleanConstant(condition) && condition.getNode().findChildByType(KtTokens.TRUE_KEYWORD) != null;
    }

    /** Returns {@code true} if {@code condition} is the boolean literal {@code false}. */
    public static boolean isFalseConstant(@Nullable KtExpression condition) {
        return isBooleanConstant(condition) && condition.getNode().findChildByType(KtTokens.FALSE_KEYWORD) != null;
    }

    /** Returns {@code true} if {@code condition} is a boolean literal ({@code true} or {@code false}). */
    public static boolean isBooleanConstant(@Nullable KtExpression condition) {
        return condition != null && condition.getNode().getElementType() == KtNodeTypes.BOOLEAN_CONSTANT;
    }

    /** Returns {@code true} if {@code declaration} has no body (and is therefore abstract). */
    public static boolean isAbstract(@NotNull KtDeclarationWithBody declaration) {
        return declaration.getBodyExpression() == null;
    }

    /** Returns the last statement of {@code expression} if it is a block, otherwise {@code expression} itself. */
    @Nullable
    public static KtExpression getExpressionOrLastStatementInBlock(@Nullable KtExpression expression) {
        if (expression instanceof KtBlockExpression) {
            return getLastStatementInABlock((KtBlockExpression) expression);
        }
        return expression;
    }

    /** Returns the last statement of the given block, or {@code null} if the block is {@code null} or empty. */
    @Nullable
    public static KtExpression getLastStatementInABlock(@Nullable KtBlockExpression blockExpression) {
        if (blockExpression == null) return null;
        List<KtExpression> statements = blockExpression.getStatements();
        return statements.isEmpty() ? null : statements.get(statements.size() - 1);
    }

    /** Returns {@code true} if {@code classOrObject} is an interface. */
    public static boolean isTrait(@NotNull KtClassOrObject classOrObject) {
        return classOrObject instanceof KtClass && ((KtClass) classOrObject).isInterface();
    }

    /**
     * Returns the outermost enclosing class or object of {@code classOrObject} (walking out through class bodies), or
     * {@code classOrObject} itself if it is top-level or local.
     */
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

    /**
     * If {@code ktParameter} is a primary-constructor {@code val}/{@code var} property parameter, returns the class or
     * object that declares it; otherwise returns {@code null}.
     */
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

    /**
     * The list of all available priorities:<p>
     *
     * 0 – for declaration and statements<p>
     * 1..12 -- for enum values of binaries<p>
     * 13 -- postfix<p>
     * 14 -- prefix<p>
     * 15 -- super and other<p>
     *
     * The suppression is used because the field is used in IntelliJ monorepo.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int MAX_PRIORITY = CollectionsKt.count(BinaryOperationPrecedence.getEntries()) + 3;

    /**
     * @return priority (that opposed to precedence) of the passed <tt>expression</tt>
     */
    private static int getPriority(@NotNull KtExpression expression) {
        if (expression instanceof KtSuperExpression) {
            return MAX_PRIORITY;
        }

        if (expression instanceof KtPostfixExpression ||
            expression instanceof KtQualifiedExpression ||
            expression instanceof KtCallExpression ||
            expression instanceof KtArrayAccessExpression ||
            expression instanceof KtDoubleColonExpression
        ) {
            return MAX_PRIORITY - 1;
        }

        if (expression instanceof KtPrefixExpression || expression instanceof KtLabeledExpression || expression instanceof KtIfExpression) {
            return MAX_PRIORITY - 2;
        }

        IElementType operation = getOperation(expression);
        if (operation instanceof KtToken) {
            BinaryOperationPrecedence binaryPrecedence = BinaryOperationPrecedence.TOKEN_TO_BINARY_PRECEDENCE_MAP.get((KtToken) operation);
            if (binaryPrecedence != null) {
                return (MAX_PRIORITY - 3) - binaryPrecedence.ordinal();
            }
        }

        if (expression instanceof KtDeclaration || expression instanceof KtStatementExpression) {
            return 0;
        }

        return MAX_PRIORITY;
    }

    /** Returns {@code true} if the parentheses in {@code expression} are redundant and could be removed. */
    @SuppressWarnings("unused") // used in intellij repo
    public static boolean areParenthesesUseless(@NotNull KtParenthesizedExpression expression) {
        KtExpression innerExpression = expression.getExpression();
        if (innerExpression == null) return true;
        PsiElement parent = expression.getParent();
        if (!(parent instanceof KtElement)) return true;
        return !areParenthesesNecessary(innerExpression, expression, (KtElement) parent);
    }

    /**
     * Returns {@code true} if parentheses around {@code innerExpression} are required for the code to keep its
     * meaning, given that they currently appear as {@code currentInner} inside {@code parentElement}. Accounts for
     * operator precedence and the many syntactic special cases where parentheses cannot be dropped.
     */
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

    /**
     * Returns {@code true} if {@code element} is an assignment ({@code =} or an augmented assignment such as
     * {@code +=}).
     */
    public static boolean isAssignment(@NotNull PsiElement element) {
        return element instanceof KtBinaryExpression &&
               KtTokens.ALL_ASSIGNMENTS.contains(((KtBinaryExpression) element).getOperationToken());
    }

    /** Returns {@code true} if {@code element} is a plain assignment ({@code =}), excluding augmented assignments. */
    public static boolean isOrdinaryAssignment(@NotNull PsiElement element) {
        return element instanceof KtBinaryExpression &&
               ((KtBinaryExpression) element).getOperationToken().equals(KtTokens.EQ);
    }

    /** Returns {@code true} if {@code expression} is a cast, whether safe ({@code as?}) or unsafe ({@code as}). */
    public static boolean isCast(@NotNull KtBinaryExpressionWithTypeRHS expression) {
        return isSafeCast(expression) || isUnsafeCast(expression);
    }

    /** Returns {@code true} if {@code expression} is a safe cast ({@code as?}). */
    public static boolean isSafeCast(@NotNull KtBinaryExpressionWithTypeRHS expression) {
        return expression.getOperationReference().getReferencedNameElementType() == KtTokens.AS_SAFE;
    }

    /** Returns {@code true} if {@code expression} is an unsafe cast ({@code as}). */
    public static boolean isUnsafeCast(@NotNull KtBinaryExpressionWithTypeRHS expression) {
        return expression.getOperationReference().getReferencedNameElementType() == KtTokens.AS_KEYWORD;
    }

    /** Returns {@code true} if {@code block} contains a variable declaration named {@code varName}. */
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

    /** Returns {@code true} if {@code whenExpression} has exactly one {@code else} branch. */
    public static boolean checkWhenExpressionHasSingleElse(@NotNull KtWhenExpression whenExpression) {
        int elseCount = 0;
        for (KtWhenEntry entry : whenExpression.getEntries()) {
            if (entry.isElse()) {
                elseCount++;
            }
        }
        return (elseCount == 1);
    }

    /** Returns the first following sibling of {@code element} that is neither whitespace nor a comment, or {@code null}. */
    @Nullable
    public static PsiElement skipTrailingWhitespacesAndComments(@Nullable PsiElement element)  {
        return PsiTreeUtil.skipSiblingsForward(element, PsiWhiteSpace.class, PsiComment.class);
    }

    /** Returns the previous leaf before {@code element}, skipping whitespace and comments, or {@code null}. */
    @Nullable
    public static PsiElement prevLeafIgnoringWhitespaceAndComments(@NotNull PsiElement element) {
        PsiElement prev = PsiTreeUtil.prevLeaf(element, true);
        while (prev != null && KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(prev.getNode().getElementType())) {
            prev = PsiTreeUtil.prevLeaf(prev, true);
        }
        return prev;
    }

    /** Returns the previous leaf if it is the identifier {@code word} (skipping whitespace and comments), or {@code null}. */
    @Nullable
    public static PsiElement getPreviousWord(@NotNull PsiElement element, @NotNull String word) {
        PsiElement prev = prevLeafIgnoringWhitespaceAndComments(element);
        if (prev != null && prev.getNode().getElementType() == KtTokens.IDENTIFIER && word.equals(prev.getText())) {
            return prev;
        }

        return null;
    }

    /** Returns the text of {@code element}, or an empty string if {@code element} is {@code null}. */
    @NotNull
    public static String getText(@Nullable PsiElement element) {
        return element != null ? element.getText() : "";
    }

    /** Returns the text of {@code element}, or {@code null} if {@code element} is {@code null}. */
    @Nullable
    public static String getNullableText(@Nullable PsiElement element) {
        return element != null ? element.getText() : null;
    }

    /**
     * CommentUtilCore.isComment fails if the element is <strong>inside</strong> a comment.
     *
     * Also, we cannot add KDocTokens to COMMENTS TokenSet, because it is used in KotlinParserDefinition.getCommentTokens(),
     * and therefore all COMMENTS tokens will be ignored by PsiBuilder.
     *
     * @param element
     * @return
     */
    public static boolean isInComment(PsiElement element) {
        return CommentUtilCore.isComment(element) || element instanceof KDocElement;
    }

    /**
     * Returns the outermost ancestor of {@code element} that is still a direct child of {@code upperBound}, or
     * {@code null}. When {@code strict} is {@code false}, {@code element} itself is considered.
     */
    @Nullable
    public static PsiElement getOutermostParent(@NotNull PsiElement element, @NotNull PsiElement upperBound, boolean strict) {
        PsiElement parent = strict ? element.getParent() : element;
        while (parent != null && parent.getParent() != upperBound) {
            parent = parent.getParent();
        }

        return parent;
    }

    /**
     * Returns the last direct child of {@code root} whose type is one of {@code elementTypes}, or {@code null} if there
     * is none.
     */
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

    /**
     * Traverses the subtree of {@code root}, collecting the outermost descendants that satisfy {@code predicate} (not
     * descending into a matching element), and returns the {@code first} or last such element, or {@code null} if there
     * is none.
     */
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

    /**
     * Returns the first direct child of {@code element} with the given node {@code type}, or {@code null} if there is
     * none.
     */
    @Nullable
    public static PsiElement findChildByType(@NotNull PsiElement element, @NotNull IElementType type) {
        ASTNode node = element.getNode().findChildByType(type);
        return node == null ? null : node.getPsi();
    }

    /** Returns the first preceding sibling of {@code element} not matched by {@code elementsToSkip}, or {@code null}. */
    @Nullable
    public static PsiElement skipSiblingsBackwardByPredicate(@Nullable PsiElement element, Predicate<PsiElement> elementsToSkip) {
        if (element == null) return null;
        for (PsiElement e = element.getPrevSibling(); e != null; e = e.getPrevSibling()) {
            if (elementsToSkip.test(e)) continue;
            return e;
        }
        return null;
    }

    /** If {@code element} is a property accessor, returns its owning property; otherwise returns {@code element} unchanged. */
    public static PsiElement ascendIfPropertyAccessor(PsiElement element) {
        if (element instanceof KtPropertyAccessor) {
            return element.getParent();
        }
        return element;
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.replaceModifierList(owner, modifierList)}
     * instead.
     */
    @Nullable
    @Contract("_, !null -> !null")
    @Deprecated
    public static KtModifierList replaceModifierList(@NotNull KtModifierListOwner owner, @Nullable KtModifierList modifierList) {
        return KtPsiMutationService.getInstance().replaceModifierList(owner, modifierList);
    }

    /** Returns the dot-separated package name of the file containing {@code element}, or {@code null} if unavailable. */
    @Nullable
    public static String getPackageName(@NotNull KtElement element) {
        KtFile file = element.getContainingKtFile();
        KtPackageDirective header = PsiTreeUtil.findChildOfType(file, KtPackageDirective.class);

        return header != null ? header.getQualifiedName() : null;
    }

    /**
     * Returns the innermost element that scopes {@code declaration} as a local declaration (a block, parameter, and so
     * on), or {@code null} if the declaration is not local. See
     * {@link #getEnclosingElementForLocalDeclaration(KtDeclaration, boolean)} with {@code skipParameters}.
     */
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

    /**
     * Returns the innermost element that scopes {@code declaration} as a local declaration, or {@code null} if it is not
     * local. When {@code skipParameters} is {@code true}, type and value parameters delegate to their owning declaration.
     */
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
            // No enclosing declaration found. There no sense to iterate through directories
            if (current instanceof PsiFile) {
                return null;
            }

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

    /** Returns {@code true} if {@code declaration} is local (declared inside a function body or other local scope). */
    public static boolean isLocal(@NotNull KtDeclaration declaration) {
        return getEnclosingElementForLocalDeclaration(declaration) != null;
    }

    /** Returns the operation token of {@code expression} (for example, {@code PLUS}), or {@code null} if unavailable. */
    @Nullable
    public static KtToken getOperationToken(@NotNull KtOperationExpression expression) {
        KtSimpleNameExpression operationExpression = expression.getOperationReference();
        IElementType elementType = operationExpression.getReferencedNameElementType();
        assert elementType == null || elementType instanceof KtToken :
                "KtOperationExpression should have operation token of type KtToken: " +
                expression;
        return (KtToken) elementType;
    }

    /** Returns {@code true} if {@code element} is a label reference expression. */
    public static boolean isLabelIdentifierExpression(PsiElement element) {
        return element instanceof KtLabelReferenceExpression;
    }

    /**
     * Returns the nearest enclosing call-like expression that {@code expression} is an operand or argument of, looking
     * through parentheses, casts, argument lists, lambdas, and labels, or {@code null} if there is none.
     */
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

    /** Returns {@code true} if {@code functionLiteral}'s enclosing lambda expression carries a label. */
    public static boolean isLabeledFunctionLiteral(@NotNull KtFunctionLiteral functionLiteral) {
        // KtFunctionLiteral -> KtLambdaExpression -> KtLabeledExpression
        return functionLiteral.getParent().getParent() instanceof KtLabeledExpression;
    }

    /**
     * Deparenthesizes {@code expression} and, if it is a block, recurses into the block's last statement (respecting
     * {@code statementFilter}), returning the innermost "result" expression.
     */
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

    /**
     * Returns {@code true} if {@code container} is an element whose children are statements (a block,
     * control-structure body, or {@code when} entry).
     */
    public static boolean isStatementContainer(@Nullable PsiElement container) {
        return container instanceof KtBlockExpression ||
               container instanceof KtContainerNodeForControlStructureBody ||
               container instanceof KtWhenEntry;
    }

    /**
     * Returns {@code true} if {@code element} appears in a statement position (its parent is a
     * {@link #isStatementContainer statement container}).
     */
    public static boolean isStatement(@NotNull PsiElement element) {
        return isStatementContainer(element.getParent());
    }

    /** Returns {@code true} if {@code binaryExpression} is an {@code in} or {@code !in} membership check. */
    public static boolean isInOrNotInOperation(@NotNull KtBinaryExpression binaryExpression) {
        return isInOperation(binaryExpression) || isNotInOperation(binaryExpression);
    }

    /** Returns {@code true} if {@code binaryExpression} is a {@code !in} (not-in) membership check. */
    public static boolean isNotInOperation(@NotNull KtBinaryExpression binaryExpression) {
        return (binaryExpression.getOperationToken() == KtTokens.NOT_IN);
    }

    private static boolean isInOperation(@NotNull KtBinaryExpression binaryExpression) {
        return (binaryExpression.getOperationToken() == KtTokens.IN_KEYWORD);
    }
}
