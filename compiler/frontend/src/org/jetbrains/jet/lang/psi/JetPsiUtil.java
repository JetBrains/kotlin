/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.kdoc.psi.api.KDocElement;
import org.jetbrains.jet.lang.parsing.JetExpressionParsing;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JetPsiUtil {

    public static final Name NO_NAME_PROVIDED = Name.special("<no name provided>");
    public static final Name ROOT_NAMESPACE_NAME = Name.special("<root namespace>");

    private JetPsiUtil() {
    }

    public static <D> void visitChildren(@NotNull JetElement element, @NotNull JetTreeVisitor<D> visitor, D data) {
        PsiElement child = element.getFirstChild();
        while (child != null) {
            if (child instanceof JetElement) {
                ((JetElement) child).accept(visitor, data);
            }
            child = child.getNextSibling();
        }
    }

    @Nullable
    public static JetExpression deparenthesizeWithNoTypeResolution(@NotNull JetExpression expression) {
        return deparenthesizeWithResolutionStrategy(expression, null);
    }

    @Nullable
    @Deprecated //Use JetPsiUtil.deparenthesizeWithNoTypeResolution() or ExpressionTypingServices.deparenthesize()
    public static JetExpression deparenthesizeWithResolutionStrategy(
            @NotNull JetExpression expression,
            @Nullable Function<JetTypeReference, Void> typeResolutionStrategy
    ) {
        if (expression instanceof JetBinaryExpressionWithTypeRHS) {
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
        else if (expression instanceof JetPrefixExpression) {
            if (JetTokens.LABELS.contains(((JetPrefixExpression) expression).getOperationReference().getReferencedNameElementType())) {
                JetExpression baseExpression = ((JetPrefixExpression) expression).getBaseExpression();
                if (baseExpression != null) {
                    expression = baseExpression;
                }
            }
        }
        if (expression instanceof JetParenthesizedExpression) {
            JetExpression innerExpression = ((JetParenthesizedExpression) expression).getExpression();
            return innerExpression != null ? deparenthesizeWithResolutionStrategy(innerExpression, typeResolutionStrategy) : null;
        }
        return expression;
    }

    @NotNull
    public static Name safeName(@Nullable String name) {
        return name == null ? NO_NAME_PROVIDED : Name.identifier(name);
    }

    @NotNull
    public static Set<JetElement> findRootExpressions(@NotNull Collection<JetElement> unreachableElements) {
        Set<JetElement> rootElements = new HashSet<JetElement>();
        final Set<JetElement> shadowedElements = new HashSet<JetElement>();
        JetVisitorVoid shadowAllChildren = new JetVisitorVoid() {
            @Override
            public void visitJetElement(JetElement element) {
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

    @NotNull
    public static FqName getFQName(@NotNull JetFile file) {
        JetNamespaceHeader header = file.getNamespaceHeader();
        return header != null ? header.getFqName() : FqName.ROOT;
    }

    @Nullable
    public static FqName getFQName(@NotNull JetNamedDeclaration namedDeclaration) {
        if (namedDeclaration instanceof JetObjectDeclarationName) {
            JetNamedDeclaration objectDeclaration = PsiTreeUtil.getParentOfType(namedDeclaration, JetObjectDeclaration.class);
            if (objectDeclaration == null) {
                objectDeclaration = PsiTreeUtil.getParentOfType(namedDeclaration, JetEnumEntry.class);
            }

            if (objectDeclaration == null) {
                return null;
            }

            return getFQName(objectDeclaration);
        }

        Name name = namedDeclaration.getNameAsName();
        if (name == null) {
            return null;
        }

        PsiElement parent = namedDeclaration.getParent();
        if (parent instanceof JetClassBody) {
            // One nesting to JetClassBody doesn't affect to qualified name
            parent = parent.getParent();
        }

        FqName firstPart = null;
        if (parent instanceof JetFile) {
            firstPart = getFQName((JetFile) parent);
        }
        else if (parent instanceof JetNamedFunction || parent instanceof JetClass) {
            firstPart = getFQName((JetNamedDeclaration) parent);
        }
        else if (namedDeclaration instanceof JetParameter) {
            JetClass constructorClass = getClassIfParameterIsProperty((JetParameter) namedDeclaration);
            if (constructorClass != null) {
                firstPart = getFQName(constructorClass);
            }
        }
        else if (parent instanceof JetObjectDeclaration) {
            if (parent.getParent() instanceof JetClassObject) {
                JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(parent, JetClassOrObject.class);
                if (classOrObject != null) {
                    firstPart = getFQName(classOrObject);
                }
            }
            else {
                firstPart = getFQName((JetNamedDeclaration) parent);
            }
        }

        if (firstPart == null) {
            return null;
        }

        return firstPart.child(name);
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
    @IfNotParsed
    public static ImportPath getImportPath(@NotNull JetImportDirective importDirective) {
        if (PsiTreeUtil.hasErrorElements(importDirective)) {
            return null;
        }

        FqName importFqn = getFQName(importDirective.getImportedReference());
        if (importFqn == null) {
            return null;
        }

        Name alias = null;
        String aliasName = importDirective.getAliasName();
        if (aliasName != null) {
            alias = Name.identifier(aliasName);
        }

        return new ImportPath(importFqn, importDirective.isAllUnder(), alias);
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

    public static boolean isImplicitlyUsed(@NotNull JetElement element) {
        PsiElement parent = element.getParent();
        if (!(parent instanceof JetBlockExpression)) return true;
        JetBlockExpression block = (JetBlockExpression) parent;
        List<JetElement> statements = block.getStatements();
        if (statements.get(statements.size() - 1) == element) {
            JetExpression expression = getDirectParentOfTypeForBlock(block, JetIfExpression.class);
            if (expression == null) {
                expression = getDirectParentOfTypeForBlock(block, JetWhenExpression.class);
            }
            if (expression == null) {
                expression = getDirectParentOfTypeForBlock(block, JetFunctionLiteral.class);
            }
            if (expression == null) {
                expression = getDirectParentOfTypeForBlock(block, JetTryExpression.class);
            }
            if (expression != null) {
                return isImplicitlyUsed(expression);
            }
        }
        return false;
    }

    public static void deleteClass(@NotNull JetClassOrObject clazz) {
        CheckUtil.checkWritable(clazz);
        JetFile file = (JetFile) clazz.getContainingFile();
        List<JetDeclaration> declarations = file.getDeclarations();
        if (declarations.size() == 1) {
            file.delete();
        }
        else {
            PsiElement parent = clazz.getParent();
            CodeEditUtil.removeChild(parent.getNode(), clazz.getNode());
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
        if (importedReference instanceof JetDotQualifiedExpression) {
            JetExpression selectorExpression = ((JetDotQualifiedExpression) importedReference).getSelectorExpression();
            return (selectorExpression instanceof JetSimpleNameExpression) ? (JetSimpleNameExpression) selectorExpression : null;
        }
        if (importedReference instanceof JetSimpleNameExpression) {
            return (JetSimpleNameExpression) importedReference;
        }
        return null;
    }

    public static boolean isFirstPartInQualified(@NotNull JetSimpleNameExpression nameExpression) {
        @SuppressWarnings("unchecked") JetUserType userType = PsiTreeUtil.getParentOfType(nameExpression, JetUserType.class, true,
                                                                                          JetDeclaration.class);
        if (userType != null) {
            return PsiTreeUtil.isAncestor(userType.getFirstChild(), nameExpression, false);
        }

        @SuppressWarnings("unchecked") JetQualifiedExpression qualifiedExpression = PsiTreeUtil.getParentOfType(nameExpression, JetQualifiedExpression.class, true, JetDeclaration.class);
        if (qualifiedExpression != null) {
            return PsiTreeUtil.isAncestor(qualifiedExpression.getFirstChild(), nameExpression, false);
        }

        return true;
    }

    public static boolean isVoidType(@Nullable JetTypeReference typeReference) {
        if (typeReference == null) {
            return false;
        }

        return KotlinBuiltIns.getInstance().getUnit().getName().asString().equals(typeReference.getText());
    }

    public static boolean isSafeCall(@NotNull Call call) {
        ASTNode callOperationNode = call.getCallOperationNode();
        return callOperationNode != null && callOperationNode.getElementType() == JetTokens.SAFE_ACCESS;
    }

    public static boolean isFunctionLiteralWithoutDeclaredParameterTypes(@Nullable JetExpression expression) {
        if (!(expression instanceof JetFunctionLiteralExpression)) return false;
        JetFunctionLiteralExpression functionLiteral = (JetFunctionLiteralExpression) expression;
        for (JetParameter parameter : functionLiteral.getValueParameters()) {
            if (parameter.getTypeReference() != null) {
                return false;
            }
        }
        return true;
    }

    public static boolean isScriptDeclaration(@NotNull JetDeclaration namedDeclaration) {
        return getScript(namedDeclaration) != null;
    }

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
    public static PsiElement getTopmostParentOfTypes(@Nullable PsiElement element, @NotNull Class<? extends PsiElement>... parentTypes) {
        if (element == null) {
            return null;
        }

        PsiElement result = null;
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (PsiTreeUtil.instanceOf(parent, parentTypes)) {
                result = parent;
            }

            parent = parent.getParent();
        }

        return result;
    }

    public static boolean isNullConstant(@NotNull JetExpression expression) {
        JetExpression deparenthesized = deparenthesizeWithNoTypeResolution(expression);
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
    private static FqName getFQName(@Nullable JetExpression expression) {
        if (expression == null) {
            return null;
        }

        if (expression instanceof JetDotQualifiedExpression) {
            JetDotQualifiedExpression dotQualifiedExpression = (JetDotQualifiedExpression) expression;
            FqName parentFqn = getFQName(dotQualifiedExpression.getReceiverExpression());
            Name child = getName(dotQualifiedExpression.getSelectorExpression());

            return parentFqn != null && child != null ? parentFqn.child(child) : null;
        }
        else if (expression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) expression;
            return FqName.topLevel(simpleNameExpression.getReferencedNameAsName());
        }
        else {
            throw new IllegalArgumentException("Can't construct fqn for: " + expression.getClass().toString());
        }
    }

    @Nullable
    private static Name getName(@Nullable JetExpression expression) {
        if (expression == null) {
            return null;
        }

        if (expression instanceof JetSimpleNameExpression) {
            return ((JetSimpleNameExpression) expression).getReferencedNameAsName();
        }
        else {
            throw new IllegalArgumentException("Can't construct name for: " + expression.getClass().toString());
        }
    }

    @Nullable
    public static JetElement getLastStatementInABlock(@Nullable JetBlockExpression blockExpression) {
        if (blockExpression == null) return null;
        List<JetElement> statements = blockExpression.getStatements();
        return statements.isEmpty() ? null : statements.get(statements.size() - 1);
    }

    public static boolean isLocalClass(@NotNull JetClassOrObject classOrObject) {
        return getOutermostClassOrObject(classOrObject) == null;
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
            if (parent instanceof JetClassObject) {
                // current class IS the class object declaration
                parent = parent.getParent();
                assert parent instanceof JetClassBody : "Parent of class object is not a class body: " + parent;
            }
            if (!(parent instanceof JetClassBody)) {
                // It is a local class, no legitimate outer
                return null;
            }

            current = (JetClassOrObject) parent.getParent();
        }
    }

    @Nullable
    public static JetClass getClassIfParameterIsProperty(@NotNull JetParameter jetParameter) {
        if (jetParameter.getValOrVarNode() != null) {
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

    private static int getPrecedenceOfOperation(@NotNull JetExpression expression, @NotNull IElementType operation) {
        if (expression instanceof JetPostfixExpression) return 0;
        if (expression instanceof JetQualifiedExpression) return 0;
        if (expression instanceof JetPrefixExpression) return 1;

        for (JetExpressionParsing.Precedence precedence : JetExpressionParsing.Precedence.values()) {
            if (precedence != JetExpressionParsing.Precedence.PREFIX && precedence != JetExpressionParsing.Precedence.POSTFIX &&
                precedence.getOperations().contains(operation)) {
                return precedence.ordinal();
            }
        }
        throw new IllegalStateException("Unknown operation");
    }

    public static boolean areParenthesesUseless(@NotNull JetParenthesizedExpression expression) {
        JetExpression innerExpression = expression.getExpression();
        JetExpression parentExpression = PsiTreeUtil.getParentOfType(expression, JetExpression.class, true);
        if (innerExpression == null || parentExpression == null) return true;

        IElementType innerOperation = getOperation(innerExpression);
        IElementType parentOperation = getOperation(parentExpression);

        // 'return (@label{...})' case
        if (parentExpression instanceof JetReturnExpression && innerOperation == JetTokens.LABEL_IDENTIFIER) {
            return false;
        }

        // '(x: Int) < y' case
        if (innerExpression instanceof JetBinaryExpressionWithTypeRHS && parentOperation == JetTokens.LT) {
            return false;
        }

        // associative operations
        if (innerOperation == parentOperation && (innerOperation == JetTokens.OROR || innerOperation == JetTokens.ANDAND)) {
            return true;
        }

        if (innerOperation == null) return true;
        if (parentExpression instanceof JetArrayAccessExpression) {
            return ((JetArrayAccessExpression) parentExpression).getArrayExpression() != expression;
        }
        if (parentOperation == null) return true;

        int innerPrecedence = getPrecedenceOfOperation(innerExpression, innerOperation);
        int parentPrecedence = getPrecedenceOfOperation(parentExpression, parentOperation);
        return innerPrecedence < parentPrecedence;
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

    @Nullable
    public static PsiElement getParentByTypeAndPredicate(
            @Nullable PsiElement element, @NotNull Class<? extends PsiElement> aClass, @NotNull Predicate<PsiElement> predicate, boolean strict) {
        if (element == null) return null;
        if (strict) {
            element = element.getParent();
        }

        while (element != null) {
            //noinspection unchecked
            if (aClass.isInstance(element) && predicate.apply(element)) {
                //noinspection unchecked
                return element;
            }
            if (element instanceof PsiFile) return null;
            element = element.getParent();
        }

        return null;
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
    public static <T extends JetElement> T getOutermostJetElement(
            @Nullable PsiElement root,
            boolean first,
            @NotNull final Class<? extends T>... elementTypes
    ) {
        if (!(root instanceof JetElement)) return null;

        final List<T> results = Lists.newArrayList();

        ((JetElement) root).accept(
                new JetVisitorVoid() {
                    @Override
                    public void visitJetElement(JetElement element) {
                        if (PsiTreeUtil.instanceOf(element, elementTypes)) {
                            //noinspection unchecked
                            results.add((T) element);
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
    public static JetExpression getCalleeExpressionIfAny(@NotNull JetExpression expression) {
        if (expression instanceof JetCallElement) {
            JetCallElement callExpression = (JetCallElement) expression;
            return callExpression.getCalleeExpression();
        }
        if (expression instanceof JetQualifiedExpression) {
            JetExpression selectorExpression = ((JetQualifiedExpression) expression).getSelectorExpression();
            if (selectorExpression != null) {
                return getCalleeExpressionIfAny(selectorExpression);
            }
        }
        if (expression instanceof JetUnaryExpression) {
            return ((JetUnaryExpression) expression).getOperationReference();
        }
        if (expression instanceof JetBinaryExpression) {
            return ((JetBinaryExpression) expression).getOperationReference();
        }
        return null;
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
}
