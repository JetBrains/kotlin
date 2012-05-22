/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.CheckUtil;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class JetPsiUtil {

    public static final String NO_NAME_PROVIDED = "<no name provided>";

    private JetPsiUtil() {
    }

    @Nullable
    public static JetExpression deparenthesize(@NotNull JetExpression expression) {
        if (expression instanceof JetBinaryExpressionWithTypeRHS) {
            JetSimpleNameExpression operationSign = ((JetBinaryExpressionWithTypeRHS) expression).getOperationSign();
            if (JetTokens.COLON.equals(operationSign.getReferencedNameElementType())) {
                expression = ((JetBinaryExpressionWithTypeRHS) expression).getLeft();
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
            return innerExpression != null ? deparenthesize(innerExpression) : null;
        }
        return expression;
    }

    @NotNull
    public static String safeName(String name) {
        return name == null ? NO_NAME_PROVIDED : name;
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

    @Nullable
    public static JetNamedFunction getSurroundingFunction(@Nullable PsiElement element) {
        while (element != null) {
            if (element instanceof JetNamedFunction) return (JetNamedFunction) element;
            if (element instanceof JetClassOrObject || element instanceof JetFile) return null;
            element = element.getParent();
        }
        return null;
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

    private static FqName getFQName(JetNamespaceHeader header) {
        StringBuilder builder = new StringBuilder();
        for (JetSimpleNameExpression nameExpression : header.getParentNamespaceNames()) {
            builder.append(nameExpression.getReferencedName());
            builder.append(".");
        }
        builder.append(header.getName());
        return new FqName(builder.toString());
    }

    public static FqName getFQName(JetFile file) {
        if (file.isScript()) {
            return FqName.ROOT;
        }
        else {
            return getFQName(file.getNamespaceHeader());
        }
    }

    @Nullable
    public static FqName getFQName(JetNamedDeclaration namedDeclaration) {
        if (namedDeclaration instanceof JetObjectDeclarationName) {
            JetObjectDeclaration objectDeclaration = PsiTreeUtil.getParentOfType(namedDeclaration, JetObjectDeclaration.class);
            if (objectDeclaration == null) {
                return null;
            }

            return getFQName(objectDeclaration);
        }

        String functionName = namedDeclaration.getName();
        if (functionName == null) {
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
        else if (parent instanceof JetNamedDeclaration) {
            firstPart = getFQName((JetNamedDeclaration) parent);
        }

        if (firstPart == null) {
            return null;
        }

        return firstPart.child(functionName);
    }

    @Nullable @JetElement.IfNotParsed
    public static ImportPath getImportPath(JetImportDirective importDirective) {
        final JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference == null) {
            return null;
        }

        final String text = importedReference.getText();
        return new ImportPath(text.replaceAll(" ", "") + (importDirective.isAllUnder() ? ".*" : ""));
    }

    @NotNull
    private static FqName makeFQName(@NotNull FqName prefix, @NotNull JetClassOrObject jetClass) {
        return prefix.child(jetClass.getName());
    }

    public static boolean isIrrefutable(JetWhenEntry entry) {
        if (entry.isElse()) return true;
        for (JetWhenCondition condition : entry.getConditions()) {
            if (condition instanceof JetWhenConditionIsPattern) {
                JetPattern pattern = ((JetWhenConditionIsPattern) condition).getPattern();
                if (pattern instanceof JetWildcardPattern) {
                    return true;
                }
                if (pattern instanceof JetBindingPattern) {
                    JetBindingPattern bindingPattern = (JetBindingPattern) pattern;
                    if (bindingPattern.getVariableDeclaration().getPropertyTypeRef() == null && bindingPattern.getCondition() == null) {
                        return true;
                    }
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
    public static String getAliasName(@NotNull JetImportDirective importDirective) {
        String aliasName = importDirective.getAliasName();
        JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference == null) {
            return null;
        }
        JetSimpleNameExpression referenceExpression = getLastReference(importedReference);
        if (aliasName == null) {
            aliasName = referenceExpression != null ? referenceExpression.getReferencedName() : null;
        }
        return aliasName;
    }

    @Nullable
    public static JetSimpleNameExpression getLastReference(@NotNull JetExpression importedReference) {
        if (importedReference instanceof JetDotQualifiedExpression) {
            JetExpression selectorExpression = ((JetDotQualifiedExpression)importedReference).getSelectorExpression();
            return (selectorExpression instanceof JetSimpleNameExpression) ? (JetSimpleNameExpression)selectorExpression : null;
        }
        if (importedReference instanceof JetSimpleNameExpression) {
            return (JetSimpleNameExpression)importedReference;
        }
        return null;
    }

    public static boolean isVoidType(@Nullable JetTypeReference typeReference) {
        if (typeReference == null) {
            return false;
        }

        return JetStandardClasses.UNIT_ALIAS.equals(typeReference.getText());
    }

    public static boolean isSafeCall(@NotNull Call call) {
        ASTNode callOperationNode = call.getCallOperationNode();
        return callOperationNode != null && callOperationNode.getElementType() == JetTokens.SAFE_ACCESS;
    }
}
