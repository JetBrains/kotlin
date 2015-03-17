/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.CheckUtil;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.codeInsight.CommentUtilCore;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.kdoc.psi.api.KDocElement;
import org.jetbrains.kotlin.lexer.JetToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.parsing.JetExpressionParsing;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JetPsiUtil {
    private JetPsiUtil() {
    }

    public interface JetExpressionWrapper {
        JetExpression getBaseExpression();
    }

    public static <D> void visitChildren(@NotNull JetElement element, @NotNull JetVisitor<Void, D> visitor, D data) {
        PsiElement child = element.getFirstChild();
        while (child != null) {
            if (child instanceof JetElement) {
                ((JetElement) child).accept(visitor, data);
            }
            child = child.getNextSibling();
        }
    }

    @NotNull
    public static JetExpression safeDeparenthesize(@NotNull JetExpression expression, boolean deparenthesizeBinaryExpressionWithTypeRHS) {
        JetExpression deparenthesized = deparenthesize(expression, deparenthesizeBinaryExpressionWithTypeRHS);
        return deparenthesized != null ? deparenthesized : expression;
    }

    @Nullable
    public static JetExpression deparenthesize(@Nullable JetExpression expression) {
        return deparenthesize(expression, /* deparenthesizeBinaryExpressionWithTypeRHS = */ true);
    }

    @Nullable
    public static JetExpression deparenthesize(
            @Nullable JetExpression expression,
            boolean deparenthesizeBinaryExpressionWithTypeRHS
    ) {
        return deparenthesizeWithResolutionStrategy(
                expression, deparenthesizeBinaryExpressionWithTypeRHS, /* deparenthesizeRecursively = */ true, null);
    }

    @Nullable
    public static JetExpression deparenthesizeOnce(
            @Nullable JetExpression expression,
            boolean deparenthesizeBinaryExpressionWithTypeRHS
    ) {
        return deparenthesizeWithResolutionStrategy(
                expression, deparenthesizeBinaryExpressionWithTypeRHS, /* deparenthesizeRecursively = */ false, null);
    }

    @Nullable
    public static JetExpression deparenthesizeWithResolutionStrategy(
            @Nullable JetExpression expression,
            boolean deparenthesizeBinaryExpressionWithTypeRHS,
            @Nullable Function<JetTypeReference, Void> typeResolutionStrategy
    ) {
        return deparenthesizeWithResolutionStrategy(expression, true, true, typeResolutionStrategy);
    }

    @Nullable
    private static JetExpression deparenthesizeWithResolutionStrategy(
            @Nullable JetExpression expression,
            boolean deparenthesizeBinaryExpressionWithTypeRHS,
            boolean deparenthesizeRecursively,
            @Nullable Function<JetTypeReference, Void> typeResolutionStrategy
    ) {
        if (deparenthesizeBinaryExpressionWithTypeRHS && expression instanceof JetBinaryExpressionWithTypeRHS) {
            JetBinaryExpressionWithTypeRHS binaryExpression = (JetBinaryExpressionWithTypeRHS) expression;
            JetSimpleNameExpression operationSign = binaryExpression.getOperationReference();
            if (JetTokens.COLON.equals(operationSign.getReferencedNameElementType())) {
                expression = binaryExpression.getLeft();
                JetTypeReference typeReference = binaryExpression.getRight();
                if (typeResolutionStrategy != null && typeReference != null) {
                    typeResolutionStrategy.apply(typeReference);
                }
            }
        }
        else if (expression instanceof JetLabeledExpression) {
            JetExpression baseExpression = ((JetLabeledExpression) expression).getBaseExpression();
            if (baseExpression != null) {
                expression = baseExpression;
            }
        }
        else if (expression instanceof JetExpressionWrapper) {
            expression = ((JetExpressionWrapper) expression).getBaseExpression();
        }
        if (expression instanceof JetParenthesizedExpression) {
            JetExpression innerExpression = ((JetParenthesizedExpression) expression).getExpression();
            return innerExpression != null && deparenthesizeRecursively ? deparenthesizeWithResolutionStrategy(
                    innerExpression, deparenthesizeBinaryExpressionWithTypeRHS, true, typeResolutionStrategy) : innerExpression;
        }
        return expression;
    }

    @NotNull
    public static Name safeName(@Nullable String name) {
        return name == null ? SpecialNames.NO_NAME_PROVIDED : Name.identifier(name);
    }

    @NotNull
    public static Set<JetElement> findRootExpressions(@NotNull Collection<JetElement> unreachableElements) {
        Set<JetElement> rootElements = new HashSet<JetElement>();
        final Set<JetElement> shadowedElements = new HashSet<JetElement>();
        JetVisitorVoid shadowAllChildren = new JetVisitorVoid() {
            @Override
            public void visitJetElement(@NotNull JetElement element) {
                if (shadowedElements.add(element)) {
                    element.acceptChildren(this);
                }
            }
        };

        for (JetElement element : unreachableElements) {
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

    /** @return <code>null</code> iff the tye has syntactic errors */
    @Nullable
    public static FqName toQualifiedName(@NotNull JetUserType userType) {
        List<String> reversedNames = Lists.newArrayList();

        JetUserType current = userType;
        while (current != null) {
            String name = current.getReferencedName();
            if (name == null) return null;

            reversedNames.add(name);
            current = current.getQualifier();
        }

        return FqName.fromSegments(ContainerUtil.reverse(reversedNames));
    }

    @Nullable
    public static Name getShortName(@NotNull JetAnnotationEntry annotation) {
        JetTypeReference typeReference = annotation.getTypeReference();
        assert typeReference != null : "Annotation entry hasn't typeReference " + annotation.getText();
        JetTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement instanceof JetUserType) {
            JetUserType userType = (JetUserType) typeElement;
            String shortName = userType.getReferencedName();
            if (shortName != null) {
                return Name.identifier(shortName);
            }
        }
        return null;
    }

    public static boolean isDeprecated(@NotNull JetModifierListOwner owner) {
        JetModifierList modifierList = owner.getModifierList();
        if (modifierList != null) {
            List<JetAnnotationEntry> annotationEntries = modifierList.getAnnotationEntries();
            for (JetAnnotationEntry annotation : annotationEntries) {
                Name shortName = getShortName(annotation);
                if (KotlinBuiltIns.getInstance().getDeprecatedAnnotation().getName().equals(shortName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static <T extends PsiElement> T getDirectParentOfTypeForBlock(@NotNull JetBlockExpression block, @NotNull Class<T> aClass) {
        T parent = PsiTreeUtil.getParentOfType(block, aClass);
        if (parent instanceof JetIfExpression) {
            JetIfExpression ifExpression = (JetIfExpression) parent;
            if (ifExpression.getElse() == block || ifExpression.getThen() == block) {
                return parent;
            }
        }
        if (parent instanceof JetWhenExpression) {
            JetWhenExpression whenExpression = (JetWhenExpression) parent;
            for (JetWhenEntry whenEntry : whenExpression.getEntries()) {
                if (whenEntry.getExpression() == block) {
                    return parent;
                }
            }
        }
        if (parent instanceof JetFunctionLiteral) {
            JetFunctionLiteral functionLiteral = (JetFunctionLiteral) parent;
            if (functionLiteral.getBodyExpression() == block) {
                return parent;
            }
        }
        if (parent instanceof JetTryExpression) {
            JetTryExpression tryExpression = (JetTryExpression) parent;
            if (tryExpression.getTryBlock() == block) {
                return parent;
            }
            for (JetCatchClause clause : tryExpression.getCatchClauses()) {
                if (clause.getCatchBody() == block) {
                    return parent;
                }
            }
        }
        return null;
    }

    public static void deleteClass(@NotNull JetClassOrObject clazz) {
        CheckUtil.checkWritable(clazz);
        JetFile file = clazz.getContainingJetFile();
        if (clazz.isLocal() || file.getDeclarations().size() > 1) {
            PsiElement parent = clazz.getParent();
            CodeEditUtil.removeChild(parent.getNode(), clazz.getNode());
        }
        else {
            file.delete();
        }
    }

    @Nullable
    public static Name getAliasName(@NotNull JetImportDirective importDirective) {
        String aliasName = importDirective.getAliasName();
        JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference == null) {
            return null;
        }
        JetSimpleNameExpression referenceExpression = getLastReference(importedReference);
        if (aliasName == null) {
            aliasName = referenceExpression != null ? referenceExpression.getReferencedName() : null;
        }

        return aliasName != null && !aliasName.isEmpty() ? Name.identifier(aliasName) : null;
    }

    @Nullable
    public static JetSimpleNameExpression getLastReference(@NotNull JetExpression importedReference) {
        JetElement selector = PsiUtilPackage.getQualifiedElementSelector(importedReference);
        return selector instanceof JetSimpleNameExpression ? (JetSimpleNameExpression) selector : null;
    }

    public static boolean isSelectorInQualified(@NotNull JetSimpleNameExpression nameExpression) {
        JetElement qualifiedElement = PsiUtilPackage.getQualifiedElement(nameExpression);
        return qualifiedElement instanceof JetQualifiedExpression
               || ((qualifiedElement instanceof JetUserType) && ((JetUserType) qualifiedElement).getQualifier() != null);
    }

    public static boolean isLHSOfDot(@NotNull JetExpression expression) {
        PsiElement parent = expression.getParent();
        if (!(parent instanceof JetQualifiedExpression)) return false;
        JetQualifiedExpression qualifiedParent = (JetQualifiedExpression) parent;
        return qualifiedParent.getReceiverExpression() == expression || isLHSOfDot(qualifiedParent);
    }

    public static boolean isVoidType(@Nullable JetTypeReference typeReference) {
        if (typeReference == null) {
            return false;
        }

        return KotlinBuiltIns.getInstance().getUnit().getName().asString().equals(typeReference.getText());
    }

    // SCRIPT: is declaration in script?
    public static boolean isScriptDeclaration(@NotNull JetDeclaration namedDeclaration) {
        return getScript(namedDeclaration) != null;
    }

    // SCRIPT: get script from top-level declaration
    @Nullable
    public static JetScript getScript(@NotNull JetDeclaration namedDeclaration) {
        PsiElement parent = namedDeclaration.getParent();
        if (parent != null && parent.getParent() instanceof JetScript) {
            return (JetScript) parent.getParent();
        }
        else {
            return null;
        }
    }

    public static boolean isVariableNotParameterDeclaration(@NotNull JetDeclaration declaration) {
        if (!(declaration instanceof JetVariableDeclaration)) return false;
        if (declaration instanceof JetProperty) return true;
        assert declaration instanceof JetMultiDeclarationEntry;
        JetMultiDeclarationEntry multiDeclarationEntry = (JetMultiDeclarationEntry) declaration;
        return !(multiDeclarationEntry.getParent().getParent() instanceof JetForExpression);
    }

    @Nullable
    public static Name getConventionName(@NotNull JetSimpleNameExpression simpleNameExpression) {
        if (simpleNameExpression.getIdentifier() != null) {
            return simpleNameExpression.getReferencedNameAsName();
        }

        PsiElement firstChild = simpleNameExpression.getFirstChild();
        if (firstChild != null) {
            IElementType elementType = firstChild.getNode().getElementType();
            if (elementType instanceof JetToken) {
                JetToken jetToken = (JetToken) elementType;
                return OperatorConventions.getNameForOperationSymbol(jetToken);
            }
        }

        return null;
    }

    @Nullable
    @Contract("null, _ -> null")
    public static PsiElement getTopmostParentOfTypes(@Nullable PsiElement element, @NotNull Class<? extends PsiElement>... parentTypes) {
        PsiElement answer = PsiTreeUtil.getParentOfType(element, parentTypes);

        do {
            PsiElement next = PsiTreeUtil.getParentOfType(answer, parentTypes);
            if (next == null) break;
            answer = next;
        }
        while (true);

        return answer;
    }

    public static boolean isNullConstant(@NotNull JetExpression expression) {
        JetExpression deparenthesized = deparenthesize(expression);
        return deparenthesized instanceof JetConstantExpression && deparenthesized.getNode().getElementType() == JetNodeTypes.NULL;
    }

    public static boolean isAbstract(@NotNull JetDeclarationWithBody declaration) {
        return declaration.getBodyExpression() == null;
    }

    public static boolean isBackingFieldReference(@NotNull JetSimpleNameExpression expression) {
        return expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER;
    }

    public static boolean isBackingFieldReference(@Nullable JetElement element) {
        return element instanceof JetSimpleNameExpression && isBackingFieldReference((JetSimpleNameExpression)element);
    }

    @Nullable
    public static JetElement getExpressionOrLastStatementInBlock(@Nullable JetExpression expression) {
        if (expression instanceof JetBlockExpression) {
            return getLastStatementInABlock((JetBlockExpression) expression);
        }
        return expression;
    }

    @Nullable
    public static JetElement getLastStatementInABlock(@Nullable JetBlockExpression blockExpression) {
        if (blockExpression == null) return null;
        List<JetElement> statements = blockExpression.getStatements();
        return statements.isEmpty() ? null : statements.get(statements.size() - 1);
    }

    public static boolean isTrait(@NotNull JetClassOrObject classOrObject) {
        return classOrObject instanceof JetClass && ((JetClass) classOrObject).isTrait();
    }

    @Nullable
    public static JetClassOrObject getOutermostClassOrObject(@NotNull JetClassOrObject classOrObject) {
        JetClassOrObject current = classOrObject;
        while (true) {
            PsiElement parent = current.getParent();
            assert classOrObject.getParent() != null : "Class with no parent: " + classOrObject.getText();

            if (parent instanceof PsiFile) {
                return current;
            }
            if (!(parent instanceof JetClassBody)) {
                // It is a local class, no legitimate outer
                return current;
            }

            current = (JetClassOrObject) parent.getParent();
        }
    }

    @Nullable
    public static JetClass getClassIfParameterIsProperty(@NotNull JetParameter jetParameter) {
        if (jetParameter.hasValOrVarNode()) {
            PsiElement parent = jetParameter.getParent();
            if (parent instanceof JetParameterList && parent.getParent() instanceof JetClass) {
                return (JetClass) parent.getParent();
            }
        }

        return null;
    }

    @Nullable
    private static IElementType getOperation(@NotNull JetExpression expression) {
        if (expression instanceof JetQualifiedExpression) {
            return ((JetQualifiedExpression) expression).getOperationSign();
        }
        else if (expression instanceof JetOperationExpression) {
            return ((JetOperationExpression) expression).getOperationReference().getReferencedNameElementType();
        }
        return null;
    }


    private static int getPriority(@NotNull JetExpression expression) {
        int maxPriority = JetExpressionParsing.Precedence.values().length + 1;

        // same as postfix operations
        if (expression instanceof JetPostfixExpression ||
            expression instanceof JetQualifiedExpression ||
            expression instanceof JetCallExpression ||
            expression instanceof JetArrayAccessExpression) {
            return maxPriority - 1;
        }

        if (expression instanceof JetPrefixExpression || expression instanceof JetLabeledExpression) return maxPriority - 2;

        if (expression instanceof JetIfExpression) {
            return JetExpressionParsing.Precedence.ASSIGNMENT.ordinal();
        }

        if (expression instanceof JetDeclaration || expression instanceof JetStatementExpression) {
            return 0;
        }

        IElementType operation = getOperation(expression);
        for (JetExpressionParsing.Precedence precedence : JetExpressionParsing.Precedence.values()) {
            if (precedence != JetExpressionParsing.Precedence.PREFIX && precedence != JetExpressionParsing.Precedence.POSTFIX &&
                precedence.getOperations().contains(operation)) {
                return maxPriority - precedence.ordinal() - 1;
            }
        }

        return maxPriority;
    }

    public static boolean areParenthesesUseless(@NotNull JetParenthesizedExpression expression) {
        JetExpression innerExpression = expression.getExpression();
        if (innerExpression == null) return true;

        PsiElement parent = expression.getParent();
        if (!(parent instanceof JetExpression)) return true;

        return !areParenthesesNecessary(innerExpression, expression, (JetExpression) parent);
    }

    public static boolean areParenthesesNecessary(@NotNull JetExpression innerExpression, @NotNull JetExpression currentInner, @NotNull JetExpression parentExpression) {
        if (parentExpression instanceof JetParenthesizedExpression || innerExpression instanceof JetParenthesizedExpression) {
            return false;
        }

        if (parentExpression instanceof JetPackageDirective) return false;

        if (parentExpression instanceof JetWhenExpression || innerExpression instanceof JetWhenExpression) {
            return false;
        }

        if (innerExpression instanceof JetIfExpression) {
            PsiElement current = parentExpression;

            while (!(current instanceof JetBlockExpression || current instanceof JetDeclaration || current instanceof JetStatementExpression)) {
                if (current.getTextRange().getEndOffset() != currentInner.getTextRange().getEndOffset()) {
                    return current.getText().charAt(current.getTextLength() - 1) != ')'; // if current expression is "guarded" by parenthesis, no extra parenthesis is necessary
                }

                current = current.getParent();
            }
        }

        IElementType innerOperation = getOperation(innerExpression);
        IElementType parentOperation = getOperation(parentExpression);

        // 'return (@label{...})' case
        if (parentExpression instanceof JetReturnExpression && innerExpression instanceof JetLabeledExpression) {
            return true;
        }

        // '(x: Int) < y' case
        if (innerExpression instanceof JetBinaryExpressionWithTypeRHS && parentOperation == JetTokens.LT) {
            return true;
        }

        int innerPriority = getPriority(innerExpression);
        int parentPriority = getPriority(parentExpression);

        if (innerPriority == parentPriority) {
            if (parentExpression instanceof JetBinaryExpression) {
                if (innerOperation == JetTokens.ANDAND || innerOperation == JetTokens.OROR) {
                    return false;
                }
                return ((JetBinaryExpression) parentExpression).getRight() == currentInner;
            }

            //'-(-x)' case
            if (parentExpression instanceof JetPrefixExpression && innerExpression instanceof JetPrefixExpression) {
                return innerOperation == parentOperation && (innerOperation == JetTokens.PLUS || innerOperation == JetTokens.MINUS);
            }
            return false;
        }

        return innerPriority < parentPriority;
    }

    public static boolean isAssignment(@NotNull PsiElement element) {
        return element instanceof JetBinaryExpression &&
               JetTokens.ALL_ASSIGNMENTS.contains(((JetBinaryExpression) element).getOperationToken());
    }

    public static boolean isOrdinaryAssignment(@NotNull PsiElement element) {
        return element instanceof JetBinaryExpression &&
               ((JetBinaryExpression) element).getOperationToken().equals(JetTokens.EQ);
    }

    @Nullable
    public static JetElement getOutermostLastBlockElement(@Nullable JetElement element, @NotNull Predicate<JetElement> checkElement) {
        if (element == null) return null;

        if (!(element instanceof JetBlockExpression)) return checkElement.apply(element) ? element : null;

        JetBlockExpression block = (JetBlockExpression)element;
        int n = block.getStatements().size();

        if (n == 0) return null;

        JetElement lastElement = block.getStatements().get(n - 1);
        return checkElement.apply(lastElement) ? lastElement : null;
    }

    public static boolean checkVariableDeclarationInBlock(@NotNull JetBlockExpression block, @NotNull String varName) {
        for (JetElement element : block.getStatements()) {
            if (element instanceof JetVariableDeclaration) {
                if (((JetVariableDeclaration) element).getNameAsSafeName().asString().equals(varName)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean checkWhenExpressionHasSingleElse(@NotNull JetWhenExpression whenExpression) {
        int elseCount = 0;
        for (JetWhenEntry entry : whenExpression.getEntries()) {
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

    public static final Predicate<JetElement> ANY_JET_ELEMENT = new Predicate<JetElement>() {
        @Override
        public boolean apply(@Nullable JetElement input) {
            return true;
        }
    };

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
     * Also, we can not add KDocTokens to COMMENTS TokenSet, because it is used in JetParserDefinition.getCommentTokens(),
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

    public static <T extends PsiElement> T getLastChildByType(@NotNull PsiElement root, @NotNull Class<? extends T>... elementTypes) {
        PsiElement[] children = root.getChildren();

        for (int i = children.length - 1; i >= 0; i--) {
            if (PsiTreeUtil.instanceOf(children[i], elementTypes)) {
                //noinspection unchecked
                return (T) children[i];
            }
        }

        return null;
    }

    @Nullable
    public static JetElement getOutermostDescendantElement(
            @Nullable PsiElement root,
            boolean first,
            final @NotNull Predicate<JetElement> predicate
    ) {
        if (!(root instanceof JetElement)) return null;

        final List<JetElement> results = Lists.newArrayList();

        root.accept(
                new JetVisitorVoid() {
                    @Override
                    public void visitJetElement(@NotNull JetElement element) {
                        if (predicate.apply(element)) {
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
            if (elementsToSkip.apply(e)) continue;
            return e;
        }
        return null;
    }

    @Nullable
    public static PsiElement skipSiblingsForwardByPredicate(@Nullable PsiElement element, Predicate<PsiElement> elementsToSkip) {
        if (element == null) return null;
        for (PsiElement e = element.getNextSibling(); e != null; e = e.getNextSibling()) {
            if (elementsToSkip.apply(e)) continue;
            return e;
        }
        return null;
    }

    // Delete given element and all the elements separating it from the neighboring elements of the same class
    public static void deleteElementWithDelimiters(@NotNull PsiElement element) {
        PsiElement paramBefore = PsiTreeUtil.getPrevSiblingOfType(element, element.getClass());

        PsiElement from;
        PsiElement to;
        if (paramBefore != null) {
            from = paramBefore.getNextSibling();
            to = element;
        }
        else {
            PsiElement paramAfter = PsiTreeUtil.getNextSiblingOfType(element, element.getClass());

            from = element;
            to = paramAfter != null ? paramAfter.getPrevSibling() : element;
        }

        PsiElement parent = element.getParent();

        parent.deleteChildRange(from, to);
    }

    // Delete element if it doesn't contain children of a given type
    public static <T extends PsiElement> void deleteChildlessElement(PsiElement element, Class<T> childClass) {
        if (PsiTreeUtil.getChildrenOfType(element, childClass) == null) {
            element.delete();
        }
    }

    public static PsiElement ascendIfPropertyAccessor(PsiElement element) {
        if (element instanceof JetPropertyAccessor) {
            return element.getParent();
        }
        return element;
    }

    @NotNull
    public static String getElementTextWithContext(@NotNull PsiElement element) {
        if (element instanceof PsiFile) {
            return element.getContainingFile().getText();
        }

        // Find parent for element among file children
        PsiElement inFileParent = PsiTreeUtil.findFirstParent(element, new Condition<PsiElement>() {
            @Override
            public boolean value(PsiElement parentCandidate) {
                return parentCandidate != null && parentCandidate.getParent() instanceof PsiFile;
            }
        });

        assert inFileParent != null : "For non-file element we should always be able to find parent in file children";

        int startContextOffset = inFileParent.getTextRange().getStartOffset();
        int elementContextOffset = element.getTextRange().getStartOffset();

        int inFileParentOffset = elementContextOffset - startContextOffset;

        return new StringBuilder(inFileParent.getText())
                .insert(inFileParentOffset, "<caret>")
                .insert(0, String.format("File name: %s\n", element.getContainingFile().getName()))
                .toString();
    }

    @Nullable
    public static JetModifierList replaceModifierList(@NotNull JetModifierListOwner owner, @Nullable JetModifierList modifierList) {
        JetModifierList oldModifierList = owner.getModifierList();
        if (modifierList == null) {
            if (oldModifierList != null) oldModifierList.delete();
            return null;
        }
        else {
            if (oldModifierList == null) {
                PsiElement firstChild = owner.getFirstChild();
                return (JetModifierList) owner.addBefore(modifierList, firstChild);
            }
            else {
                return (JetModifierList) oldModifierList.replace(modifierList);
            }
        }
    }

    @Nullable
    public static String getPackageName(@NotNull JetElement element) {
        JetFile file = element.getContainingJetFile();
        JetPackageDirective header = PsiTreeUtil.findChildOfType(file, JetPackageDirective.class);

        return header != null ? header.getQualifiedName() : null;
    }

    @Nullable
    public static JetElement getEnclosingElementForLocalDeclaration(@NotNull JetDeclaration declaration) {
        return getEnclosingElementForLocalDeclaration(declaration, true);
    }

    @Nullable
    public static JetElement getEnclosingElementForLocalDeclaration(@NotNull JetDeclaration declaration, boolean skipParameters) {
        if (declaration instanceof JetTypeParameter && skipParameters) {
            declaration = PsiTreeUtil.getParentOfType(declaration, JetNamedDeclaration.class);
        }
        else if (declaration instanceof JetParameter) {
            if (((JetParameter) declaration).getValOrVarNode() != null) return null;

            PsiElement parent = declaration.getParent();
            if (skipParameters && parent != null && parent.getParent() instanceof JetNamedFunction) {
                declaration = (JetNamedFunction) parent.getParent();
            }
        }

        // No appropriate stub-tolerant method in PsiTreeUtil, nor JetStubbedPsiUtil, writing manually
        PsiElement current = PsiTreeUtil.getStubOrPsiParent(declaration);
        while (current != null) {
            PsiElement parent = PsiTreeUtil.getStubOrPsiParent(current);
            if (parent instanceof JetScript) return null;
            if (current instanceof JetClassInitializer) {
                return ((JetClassInitializer) current).getBody();
            }
            if (current instanceof JetBlockExpression ||
                current instanceof JetProperty ||
                current instanceof JetParameter ||
                current instanceof JetFunction) {
                return (JetElement) current;
            }

            current = parent;
        }
        return null;
    }

    public static boolean isLocal(@NotNull JetDeclaration declaration) {
        return getEnclosingElementForLocalDeclaration(declaration) != null;
    }

    @Nullable
    public static JetToken getOperationToken(@NotNull JetOperationExpression expression) {
        JetSimpleNameExpression operationExpression = expression.getOperationReference();
        IElementType elementType = operationExpression.getReferencedNameElementType();
        assert elementType == null || elementType instanceof JetToken :
                "JetOperationExpression should have operation token of type JetToken: " +
                expression;
        return (JetToken) elementType;
    }

    public static boolean isLabelIdentifierExpression(PsiElement element) {
        return element instanceof JetSimpleNameExpression &&
               ((JetSimpleNameExpression) element).getReferencedNameElementType() == JetTokens.LABEL_IDENTIFIER;
    }

    @Nullable
    public static JetExpression getParentCallIfPresent(@NotNull JetExpression expression) {
        PsiElement parent = expression.getParent();
        while (parent != null) {
            if (parent instanceof JetBinaryExpression ||
                parent instanceof JetUnaryExpression ||
                parent instanceof JetLabeledExpression ||
                parent instanceof JetDotQualifiedExpression ||
                parent instanceof JetCallExpression ||
                parent instanceof JetArrayAccessExpression ||
                parent instanceof JetMultiDeclaration) {

                if (parent instanceof JetLabeledExpression) {
                    parent = parent.getParent();
                    continue;
                }

                //check that it's in inlineable call would be in resolve call of parent
                return (JetExpression) parent;
            }
            else if (parent instanceof JetParenthesizedExpression || parent instanceof JetBinaryExpressionWithTypeRHS) {
                parent = parent.getParent();
            }
            else if (parent instanceof JetValueArgument || parent instanceof JetValueArgumentList) {
                parent = parent.getParent();
            }
            else {
                return null;
            }
        }
        return null;
    }
}
