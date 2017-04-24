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

package org.jetbrains.kotlin.idea.codeInsight.surroundWith;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils;
import org.jetbrains.kotlin.idea.core.ShortenReferences;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.KotlinTypeKt;

import java.util.ArrayList;
import java.util.List;

public class MoveDeclarationsOutHelper {

    public static PsiElement[] move(@NotNull PsiElement container, @NotNull PsiElement[] statements, boolean generateDefaultInitializers) {
        if (statements.length == 0) {
            return statements;
        }

        Project project = container.getProject();

        List<PsiElement> resultStatements = new ArrayList<PsiElement>();
        List<KtProperty> propertiesDeclarations = new ArrayList<KtProperty>();

        // Dummy element to add new declarations at the beginning
        KtPsiFactory psiFactory = KtPsiFactoryKt.KtPsiFactory(project);
        PsiElement dummyFirstStatement = container.addBefore(psiFactory.createExpression("dummyStatement"), statements[0]);

        try {
            SearchScope scope = new LocalSearchScope(container);
            int lastStatementOffset = statements[statements.length - 1].getTextRange().getEndOffset();

            for (PsiElement statement : statements) {
                if (needToDeclareOut(statement, lastStatementOffset, scope)) {
                    if (statement instanceof KtProperty && ((KtProperty) statement).getInitializer() != null) {
                        KtProperty property = (KtProperty) statement;
                        KtProperty declaration = createVariableDeclaration(property, generateDefaultInitializers);
                        declaration = (KtProperty) container.addBefore(declaration, dummyFirstStatement);
                        propertiesDeclarations.add(declaration);
                        container.addAfter(psiFactory.createNewLine(), declaration);

                        KtBinaryExpression assignment = createVariableAssignment(property);
                        resultStatements.add(property.replace(assignment));
                    }
                    else {
                        PsiElement newStatement = container.addBefore(statement, dummyFirstStatement);
                        container.addAfter(psiFactory.createNewLine(), newStatement);
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

        ShortenReferences.DEFAULT.process(propertiesDeclarations);

        return PsiUtilCore.toPsiElementArray(resultStatements);
    }

    @NotNull
    private static KtBinaryExpression createVariableAssignment(@NotNull KtProperty property) {
        String propertyName = property.getName();
        assert propertyName != null : "Property should have a name " + property.getText();
        KtBinaryExpression assignment = (KtBinaryExpression) KtPsiFactoryKt
                .KtPsiFactory(property).createExpression(propertyName + " = x");
        KtExpression right = assignment.getRight();
        assert right != null : "Created binary expression should have a right part " + assignment.getText();
        KtExpression initializer = property.getInitializer();
        assert initializer != null : "Initializer should exist for property " + property.getText();
        right.replace(initializer);
        return assignment;
    }

    @NotNull
    private static KtProperty createVariableDeclaration(@NotNull KtProperty property, boolean generateDefaultInitializers) {
        KotlinType propertyType = getPropertyType(property);
        String defaultInitializer = null;
        if (generateDefaultInitializers && property.isVar()) {
            defaultInitializer = CodeInsightUtils.defaultInitializer(propertyType);
        }
        return createProperty(property, propertyType, defaultInitializer);
    }

    @NotNull
    private static KotlinType getPropertyType(@NotNull KtProperty property) {
        BindingContext bindingContext = ResolutionUtils.analyze(property, BodyResolveMode.PARTIAL);

        VariableDescriptor propertyDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
        assert propertyDescriptor != null : "Couldn't resolve property to property descriptor " + property.getText();
        return propertyDescriptor.getType();
    }

    @NotNull
    private static KtProperty createProperty(@NotNull KtProperty property, @NotNull KotlinType propertyType, @Nullable String initializer) {
        KtTypeReference typeRef = property.getTypeReference();
        String typeString = null;
        if (typeRef != null) {
            typeString = typeRef.getText();
        }
        else if (!KotlinTypeKt.isError(propertyType)) {
            typeString = IdeDescriptorRenderers.SOURCE_CODE.renderType(propertyType);
        }

        return KtPsiFactoryKt.KtPsiFactory(property).createProperty(property.getName(), typeString, property.isVar(), initializer);
    }

    private static boolean needToDeclareOut(@NotNull PsiElement element, int lastStatementOffset, @NotNull SearchScope scope) {
        if (element instanceof KtProperty ||
            element instanceof KtClassOrObject ||
            element instanceof KtFunction) {

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
