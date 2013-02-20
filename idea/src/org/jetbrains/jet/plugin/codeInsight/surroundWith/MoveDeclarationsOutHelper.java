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

package org.jetbrains.jet.plugin.codeInsight.surroundWith;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils;
import org.jetbrains.jet.plugin.codeInsight.ReferenceToClassesShortening;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.lang.psi.JetPsiFactory.*;

public class MoveDeclarationsOutHelper {

    public static PsiElement[] move(@NotNull PsiElement container, @NotNull PsiElement[] statements, boolean generateDefaultInitializers) {
        if (statements.length == 0) {
            return statements;
        }

        Project project = container.getProject();

        List<PsiElement> resultStatements = new ArrayList<PsiElement>();
        List<JetProperty> propertiesDeclarations = new ArrayList<JetProperty>();

        // Dummy element to add new declarations at the beginning
        PsiElement dummyFirstStatement = container.addBefore(createExpression(project, "dummyStatement "), statements[0]);

        try {
            SearchScope scope = new LocalSearchScope(container);
            int lastStatementOffset = statements[statements.length - 1].getTextRange().getEndOffset();

            for (PsiElement statement : statements) {
                if (needToDeclareOut(statement, lastStatementOffset, scope)) {
                    if (statement instanceof JetProperty && ((JetProperty) statement).getInitializer() != null) {
                        JetProperty property = (JetProperty) statement;
                        JetProperty declaration = createVariableDeclaration(property, generateDefaultInitializers);
                        declaration = (JetProperty) container.addBefore(declaration, dummyFirstStatement);
                        propertiesDeclarations.add(declaration);
                        container.addAfter(createNewLine(project), declaration);

                        JetBinaryExpression assignment = createVariableAssignment(property);
                        resultStatements.add(property.replace(assignment));
                    }
                    else {
                        PsiElement newStatement = container.addBefore(statement, dummyFirstStatement);
                        container.addAfter(createNewLine(project), newStatement);
                        container.deleteChildRange(statement, statement);
                    }
                }
                else {
                    resultStatements.add(statement);
                }
            }
        }
        finally {
            dummyFirstStatement.delete();
        }

        ReferenceToClassesShortening.compactReferenceToClasses(propertiesDeclarations);

        return PsiUtilCore.toPsiElementArray(resultStatements);
    }

    @NotNull
    private static JetBinaryExpression createVariableAssignment(@NotNull JetProperty property) {
        String propertyName = property.getName();
        assert propertyName != null : "Property should have a name " + property.getText();
        JetBinaryExpression assignment = (JetBinaryExpression) createExpression(property.getProject(), propertyName + " = x");
        JetExpression right = assignment.getRight();
        assert right != null : "Created binary expression should have a right part " + assignment.getText();
        JetExpression initializer = property.getInitializer();
        assert initializer != null : "Initializer should exist for property " + property.getText();
        right.replace(initializer);
        return assignment;
    }

    @NotNull
    private static JetProperty createVariableDeclaration(@NotNull JetProperty property, boolean generateDefaultInitializers) {
        JetType propertyType = getPropertyType(property);
        String defaultInitializer = null;
        if (generateDefaultInitializers && property.isVar()) {
            defaultInitializer = CodeInsightUtils.defaultInitializer(propertyType);
        }
        return createProperty(property, propertyType, defaultInitializer);
    }

    @NotNull
    private static JetType getPropertyType(@NotNull JetProperty property) {
        ResolveSession resolveSession = WholeProjectAnalyzerFacade.getLazyResolveSessionForFile((JetFile) property.getContainingFile());
        BindingContext expressionBindingContext = ResolveSessionUtils.resolveToExpression(resolveSession, property);

        VariableDescriptor propertyDescriptor = expressionBindingContext.get(BindingContext.VARIABLE, property);
        assert propertyDescriptor != null : "Couldn't resolve property to property descriptor " + property.getText();
        return propertyDescriptor.getType();
    }

    @NotNull
    private static JetProperty createProperty(@NotNull JetProperty property, @NotNull JetType propertyType, @Nullable String initializer) {
        JetTypeReference typeRef = property.getTypeRef();
        String typeString = null;
        if (typeRef != null) {
            typeString = typeRef.getText();
        }
        else if (!ErrorUtils.isErrorType(propertyType)) {
            typeString = DescriptorRenderer.TEXT.renderType(propertyType);
        }

        return JetPsiFactory.createProperty(property.getProject(), property.getName(), typeString, property.isVar(), initializer);
    }

    private static boolean needToDeclareOut(@NotNull PsiElement element, int lastStatementOffset, @NotNull SearchScope scope) {
        if (element instanceof JetProperty ||
            element instanceof JetClassOrObject ||
            element instanceof JetFunction) {

            // Descriptor for local object is linked with JetObjectNameDeclaration
            if (element instanceof JetObjectDeclaration) {
                JetObjectDeclarationName declarationName = ((JetObjectDeclaration) element).getNameAsDeclaration();
                if (declarationName != null) {
                    element = declarationName;
                }
            }

            PsiReference[] refs = ReferencesSearch.search(element, scope, false).toArray(PsiReference.EMPTY_ARRAY);
            if (refs.length > 0) {
                PsiReference lastRef = refs[refs.length - 1];
                if (lastRef.getElement().getTextOffset() > lastStatementOffset) {
                    return true;
                }
            }
        }
        return false;
    }

    private MoveDeclarationsOutHelper() {
    }
}
