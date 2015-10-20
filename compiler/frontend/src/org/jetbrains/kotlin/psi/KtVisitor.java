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

import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class KtVisitor<R, D> extends PsiElementVisitor {
    public R visitJetElement(@NotNull KtElement element, D data) {
        visitElement(element);
        return null;
    }

    public R visitDeclaration(@NotNull KtDeclaration dcl, D data) {
        return visitExpression(dcl, data);
    }

    public R visitClass(@NotNull KtClass klass, D data) {
        return visitClassOrObject(klass, data);
    }

    public R visitObjectDeclaration(@NotNull KtObjectDeclaration declaration, D data) {
        return visitClassOrObject(declaration, data);
    }

    public R visitClassOrObject(@NotNull KtClassOrObject classOrObject, D data) {
        return visitNamedDeclaration(classOrObject, data);
    }

    public R visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor, D data) {
        return visitDeclaration(constructor, data);
    }

    public R visitPrimaryConstructor(@NotNull KtPrimaryConstructor constructor, D data) {
        return visitDeclaration(constructor, data);
    }

    public R visitNamedFunction(@NotNull KtNamedFunction function, D data) {
        return visitNamedDeclaration(function, data);
    }

    public R visitProperty(@NotNull KtProperty property, D data) {
        return visitNamedDeclaration(property, data);
    }

    public R visitMultiDeclaration(@NotNull KtMultiDeclaration multiDeclaration, D data) {
        return visitDeclaration(multiDeclaration, data);
    }

    public R visitMultiDeclarationEntry(@NotNull KtMultiDeclarationEntry multiDeclarationEntry, D data) {
        return visitNamedDeclaration(multiDeclarationEntry, data);
    }

    public R visitTypedef(@NotNull KtTypedef typedef, D data) {
        return visitNamedDeclaration(typedef, data);
    }

    public R visitJetFile(@NotNull KtFile file, D data) {
        visitFile(file);
        return null;
    }

    public R visitScript(@NotNull KtScript script, D data) {
        return visitDeclaration(script, data);
    }

    public R visitImportDirective(@NotNull KtImportDirective importDirective, D data) {
        return visitJetElement(importDirective, data);
    }

    public R visitImportList(@NotNull KtImportList importList, D data) {
        return visitJetElement(importList, data);
    }

    public R visitFileAnnotationList(@NotNull KtFileAnnotationList fileAnnotationList, D data) {
        return visitJetElement(fileAnnotationList, data);
    }

    public R visitClassBody(@NotNull KtClassBody classBody, D data) {
        return visitJetElement(classBody, data);
    }

    public R visitModifierList(@NotNull KtModifierList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitAnnotation(@NotNull KtAnnotation annotation, D data) {
        return visitJetElement(annotation, data);
    }

    public R visitAnnotationEntry(@NotNull KtAnnotationEntry annotationEntry, D data) {
        return visitJetElement(annotationEntry, data);
    }

    public R visitAnnotationUseSiteTarget(@NotNull KtAnnotationUseSiteTarget annotationTarget, D data) {
        return visitJetElement(annotationTarget, data);
    }

    public R visitConstructorCalleeExpression(@NotNull KtConstructorCalleeExpression constructorCalleeExpression, D data) {
        return visitJetElement(constructorCalleeExpression, data);
    }

    public R visitTypeParameterList(@NotNull KtTypeParameterList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitTypeParameter(@NotNull KtTypeParameter parameter, D data) {
        return visitNamedDeclaration(parameter, data);
    }

    public R visitEnumEntry(@NotNull KtEnumEntry enumEntry, D data) {
        return visitClass(enumEntry, data);
    }

    public R visitParameterList(@NotNull KtParameterList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitParameter(@NotNull KtParameter parameter, D data) {
        return visitNamedDeclaration(parameter, data);
    }

    public R visitDelegationSpecifierList(@NotNull KtDelegationSpecifierList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitDelegationSpecifier(@NotNull KtDelegationSpecifier specifier, D data) {
        return visitJetElement(specifier, data);
    }

    public R visitDelegationByExpressionSpecifier(@NotNull KtDelegatorByExpressionSpecifier specifier, D data) {
        return visitDelegationSpecifier(specifier, data);
    }

    public R visitDelegationToSuperCallSpecifier(@NotNull KtDelegatorToSuperCall call, D data) {
        return visitDelegationSpecifier(call, data);
    }

    public R visitDelegationToSuperClassSpecifier(@NotNull KtDelegatorToSuperClass specifier, D data) {
        return visitDelegationSpecifier(specifier, data);
    }

    public R visitConstructorDelegationCall(@NotNull KtConstructorDelegationCall call, D data) {
        return visitJetElement(call, data);
    }

    public R visitPropertyDelegate(@NotNull KtPropertyDelegate delegate, D data) {
        return visitJetElement(delegate, data);
    }

    public R visitTypeReference(@NotNull KtTypeReference typeReference, D data) {
        return visitJetElement(typeReference, data);
    }

    public R visitValueArgumentList(@NotNull KtValueArgumentList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitArgument(@NotNull KtValueArgument argument, D data) {
        return visitJetElement(argument, data);
    }

    public R visitExpression(@NotNull KtExpression expression, D data) {
        return visitJetElement(expression, data);
    }

    public R visitLoopExpression(@NotNull KtLoopExpression loopExpression, D data) {
        return visitExpression(loopExpression, data);
    }

    public R visitConstantExpression(@NotNull KtConstantExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression, D data) {
        return visitReferenceExpression(expression, data);
    }

    public R visitReferenceExpression(@NotNull KtReferenceExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitLabeledExpression(@NotNull KtLabeledExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitPrefixExpression(@NotNull KtPrefixExpression expression, D data) {
        return visitUnaryExpression(expression, data);
    }

    public R visitPostfixExpression(@NotNull KtPostfixExpression expression, D data) {
        return visitUnaryExpression(expression, data);
    }

    public R visitUnaryExpression(@NotNull KtUnaryExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitBinaryExpression(@NotNull KtBinaryExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitReturnExpression(@NotNull KtReturnExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitExpressionWithLabel(@NotNull KtExpressionWithLabel expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitThrowExpression(@NotNull KtThrowExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitBreakExpression(@NotNull KtBreakExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitContinueExpression(@NotNull KtContinueExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitIfExpression(@NotNull KtIfExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitWhenExpression(@NotNull KtWhenExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitTryExpression(@NotNull KtTryExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitForExpression(@NotNull KtForExpression expression, D data) {
        return visitLoopExpression(expression, data);
    }

    public R visitWhileExpression(@NotNull KtWhileExpression expression, D data) {
        return visitLoopExpression(expression, data);
    }

    public R visitDoWhileExpression(@NotNull KtDoWhileExpression expression, D data) {
        return visitLoopExpression(expression, data);
    }

    public R visitFunctionLiteralExpression(@NotNull KtFunctionLiteralExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitAnnotatedExpression(@NotNull KtAnnotatedExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitCallExpression(@NotNull KtCallExpression expression, D data) {
        return visitReferenceExpression(expression, data);
    }

    public R visitArrayAccessExpression(@NotNull KtArrayAccessExpression expression, D data) {
        return visitReferenceExpression(expression, data);
    }

    public R visitQualifiedExpression(@NotNull KtQualifiedExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitDoubleColonExpression(@NotNull KtDoubleColonExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitCallableReferenceExpression(@NotNull KtCallableReferenceExpression expression, D data) {
        return visitDoubleColonExpression(expression, data);
    }

    public R visitClassLiteralExpression(@NotNull KtClassLiteralExpression expression, D data) {
        return visitDoubleColonExpression(expression, data);
    }

    public R visitDotQualifiedExpression(@NotNull KtDotQualifiedExpression expression, D data) {
        return visitQualifiedExpression(expression, data);
    }

    public R visitSafeQualifiedExpression(@NotNull KtSafeQualifiedExpression expression, D data) {
        return visitQualifiedExpression(expression, data);
    }

    public R visitObjectLiteralExpression(@NotNull KtObjectLiteralExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitRootPackageExpression(@NotNull KtRootPackageExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitBlockExpression(@NotNull KtBlockExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitCatchSection(@NotNull KtCatchClause catchClause, D data) {
        return visitJetElement(catchClause, data);
    }

    public R visitFinallySection(@NotNull KtFinallySection finallySection, D data) {
        return visitJetElement(finallySection, data);
    }

    public R visitTypeArgumentList(@NotNull KtTypeArgumentList typeArgumentList, D data) {
        return visitJetElement(typeArgumentList, data);
    }

    public R visitThisExpression(@NotNull KtThisExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitSuperExpression(@NotNull KtSuperExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitParenthesizedExpression(@NotNull KtParenthesizedExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitInitializerList(@NotNull KtInitializerList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitAnonymousInitializer(@NotNull KtClassInitializer initializer, D data) {
        return visitDeclaration(initializer, data);
    }

    public R visitPropertyAccessor(@NotNull KtPropertyAccessor accessor, D data) {
        return visitDeclaration(accessor, data);
    }

    public R visitTypeConstraintList(@NotNull KtTypeConstraintList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitTypeConstraint(@NotNull KtTypeConstraint constraint, D data) {
        return visitJetElement(constraint, data);
    }

    private R visitTypeElement(@NotNull KtTypeElement type, D data) {
        return visitJetElement(type, data);
    }

    public R visitUserType(@NotNull KtUserType type, D data) {
        return visitTypeElement(type, data);
    }

    public R visitDynamicType(@NotNull KtDynamicType type, D data) {
        return visitTypeElement(type, data);
    }

    public R visitFunctionType(@NotNull KtFunctionType type, D data) {
        return visitTypeElement(type, data);
    }

    public R visitSelfType(@NotNull KtSelfType type, D data) {
        return visitTypeElement(type, data);
    }

    public R visitBinaryWithTypeRHSExpression(@NotNull KtBinaryExpressionWithTypeRHS expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitStringTemplateExpression(@NotNull KtStringTemplateExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitNamedDeclaration(@NotNull KtNamedDeclaration declaration, D data) {
        return visitDeclaration(declaration, data);
    }

    public R visitNullableType(@NotNull KtNullableType nullableType, D data) {
        return visitTypeElement(nullableType, data);
    }

    public R visitTypeProjection(@NotNull KtTypeProjection typeProjection, D data) {
        return visitJetElement(typeProjection, data);
    }

    public R visitWhenEntry(@NotNull KtWhenEntry jetWhenEntry, D data) {
        return visitJetElement(jetWhenEntry, data);
    }

    public R visitIsExpression(@NotNull KtIsExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitWhenConditionIsPattern(@NotNull KtWhenConditionIsPattern condition, D data) {
        return visitJetElement(condition, data);
    }

    public R visitWhenConditionInRange(@NotNull KtWhenConditionInRange condition, D data) {
        return visitJetElement(condition, data);
    }

    public R visitWhenConditionWithExpression(@NotNull KtWhenConditionWithExpression condition, D data) {
        return visitJetElement(condition, data);
    }

    public R visitObjectDeclarationName(@NotNull KtObjectDeclarationName declarationName, D data) {
        return visitExpression(declarationName, data);
    }

    public R visitStringTemplateEntry(@NotNull KtStringTemplateEntry entry, D data) {
        return visitJetElement(entry, data);
    }

    public R visitStringTemplateEntryWithExpression(@NotNull KtStringTemplateEntryWithExpression entry, D data) {
        return visitStringTemplateEntry(entry, data);
    }

    public R visitBlockStringTemplateEntry(@NotNull KtBlockStringTemplateEntry entry, D data) {
        return visitStringTemplateEntryWithExpression(entry, data);
    }

    public R visitSimpleNameStringTemplateEntry(@NotNull KtSimpleNameStringTemplateEntry entry, D data) {
        return visitStringTemplateEntryWithExpression(entry, data);
    }

    public R visitLiteralStringTemplateEntry(@NotNull KtLiteralStringTemplateEntry entry, D data) {
        return visitStringTemplateEntry(entry, data);
    }

    public R visitEscapeStringTemplateEntry(@NotNull KtEscapeStringTemplateEntry entry, D data) {
        return visitStringTemplateEntry(entry, data);
    }

    public R visitPackageDirective(@NotNull KtPackageDirective directive, D data) {
        return visitJetElement(directive, data);
    }
}
