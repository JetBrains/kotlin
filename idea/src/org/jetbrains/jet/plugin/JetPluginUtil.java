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

package org.jetbrains.jet.plugin;

import com.google.common.collect.Lists;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.parsing.JetExpressionParsing;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.LinkedList;

public class JetPluginUtil {
    @NotNull
    private static LinkedList<String> computeTypeFullNameList(JetType type) {
        if (type instanceof DeferredType) {
            type = ((DeferredType)type).getActualType();
        }
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();

        LinkedList<String> fullName = Lists.newLinkedList();
        while (declarationDescriptor != null && !(declarationDescriptor instanceof ModuleDescriptor)) {
            fullName.addFirst(declarationDescriptor.getName().getName());
            declarationDescriptor = declarationDescriptor.getContainingDeclaration();
        }
        assert fullName.size() > 0;
        if (JavaDescriptorResolver.JAVA_ROOT.getName().equals(fullName.getFirst())) {
            fullName.removeFirst();
        }
        return fullName;
    }

    public static boolean checkTypeIsStandard(JetType type, Project project) {
        if (KotlinBuiltIns.getInstance().isAny(type) || KotlinBuiltIns.getInstance().isNothingOrNullableNothing(type) || KotlinBuiltIns.getInstance().isUnit(type) ||
             KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(type)) {
            return true;
        }

        LinkedList<String> fullName = computeTypeFullNameList(type);
        if (fullName.size() == 3 && fullName.getFirst().equals("java") && fullName.get(1).equals("lang")) {
            return true;
        }

        JetScope libraryScope = KotlinBuiltIns.getInstance().getBuiltInsScope();

        DeclarationDescriptor declaration = type.getMemberScope().getContainingDeclaration();
        if (ErrorUtils.isError(declaration)) {
            return false;
        }
        while (!(declaration instanceof NamespaceDescriptor)) {
            declaration = declaration.getContainingDeclaration();
            assert declaration != null;
        }
        return libraryScope == ((NamespaceDescriptor) declaration).getMemberScope();
    }

    @NotNull
    public static String getPluginVersion() {
        IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("org.jetbrains.kotlin"));
        assert plugin != null : "How can it be? Kotlin plugin is available, but its component is running. Complete nonsense.";
        return plugin.getVersion();
    }

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
        if (parentExpression instanceof JetArrayAccessExpression) return false;
        if (parentOperation == null) return true;

        int innerPrecedence = getPrecedenceOfOperation(innerExpression, innerOperation);
        int parentPrecedence = getPrecedenceOfOperation(parentExpression, parentOperation);
        return innerPrecedence < parentPrecedence;
    }
}
