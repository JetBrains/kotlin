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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.CheckUtil;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
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

/**
 * @author abreslav
 */
public class JetPsiUtil {

    public static final Name NO_NAME_PROVIDED = Name.special("<no name provided>");
    public static final Name ROOT_NAMESPACE_NAME = Name.special("<root namespace>");

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

    public static FqName getFQName(JetFile file) {
        JetNamespaceHeader header = file.getNamespaceHeader();
        return header != null ? header.getFqName() : FqName.ROOT;
    }

    @Nullable
    public static FqName getFQName(JetNamedDeclaration namedDeclaration) {
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
        else if (parent instanceof JetObjectDeclaration) {
            if (parent.getParent() instanceof JetClassObject) {
                JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(parent, JetClassOrObject.class);
                if (classOrObject != null) {
                    firstPart = getFQName((JetNamedDeclaration) classOrObject);
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

    @Nullable
    public static Name getShortName(JetAnnotationEntry annotation) {
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

    public static boolean isDeprecated(JetModifierListOwner owner) {
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
    public static ImportPath getImportPath(JetImportDirective importDirective) {
        final JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference == null) {
            return null;
        }

        if (PsiTreeUtil.hasErrorElements(importedReference)) {
            return null;
        }

        final String text = importedReference.getText();
        return new ImportPath(text.replaceAll(" ", "") + (importDirective.isAllUnder() ? ".*" : ""));
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

        //noinspection ConstantConditions
        return StringUtil.isNotEmpty(aliasName) ? Name.identifier(aliasName) : null;
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

    public static boolean isVoidType(@Nullable JetTypeReference typeReference) {
        if (typeReference == null) {
            return false;
        }

        return KotlinBuiltIns.UNIT_ALIAS.getName().equals(typeReference.getText());
    }

    public static boolean isSafeCall(@NotNull Call call) {
        ASTNode callOperationNode = call.getCallOperationNode();
        return callOperationNode != null && callOperationNode.getElementType() == JetTokens.SAFE_ACCESS;
    }

    public static boolean isFunctionLiteralWithoutDeclaredParameterTypes(JetExpression expression) {
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
        JetExpression deparenthesized = deparenthesize(expression);
        return deparenthesized instanceof JetConstantExpression && deparenthesized.getNode().getElementType() == JetNodeTypes.NULL;
    }

    public static boolean isAbstract(@NotNull JetDeclarationWithBody declaration) {
        return declaration.getBodyExpression() == null;
    }
}
