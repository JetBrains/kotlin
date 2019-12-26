/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;

public class KtVisitorVoid extends KtVisitor<Void, Void> {
    // methods with void return

    public void visitKtElement(@NotNull KtElement element) {
        super.visitKtElement(element, null);
    }

    public void visitDeclaration(@NotNull KtDeclaration dcl) {
        super.visitDeclaration(dcl, null);
    }

    public void visitClass(@NotNull KtClass klass) {
        super.visitClass(klass, null);
    }

    public void visitClassOrObject(@NotNull KtClassOrObject classOrObject) {
        super.visitClassOrObject(classOrObject, null);
    }

    public void visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor) {
        super.visitSecondaryConstructor(constructor, null);
    }

    public void visitPrimaryConstructor(@NotNull KtPrimaryConstructor constructor) {
        super.visitPrimaryConstructor(constructor, null);
    }

    public void visitNamedFunction(@NotNull KtNamedFunction function) {
        super.visitNamedFunction(function, null);
    }

    public void visitProperty(@NotNull KtProperty property) {
        super.visitProperty(property, null);
    }

    public void visitTypeAlias(@NotNull KtTypeAlias typeAlias) {
        super.visitTypeAlias(typeAlias, null);
    }

    public void visitDestructuringDeclaration(@NotNull KtDestructuringDeclaration destructuringDeclaration) {
        super.visitDestructuringDeclaration(destructuringDeclaration, null);
    }

    public void visitDestructuringDeclarationEntry(@NotNull KtDestructuringDeclarationEntry multiDeclarationEntry) {
        super.visitDestructuringDeclarationEntry(multiDeclarationEntry, null);
    }

    public void visitKtFile(@NotNull KtFile file) {
        super.visitKtFile(file, null);
    }

    public void visitScript(@NotNull KtScript script) {
        super.visitScript(script, null);
    }

    public void visitImportAlias(@NotNull KtImportAlias importAlias) {
        super.visitImportAlias(importAlias, null);
    }

    public void visitImportDirective(@NotNull KtImportDirective importDirective) {
        super.visitImportDirective(importDirective, null);
    }

    public void visitImportList(@NotNull KtImportList importList) {
        super.visitImportList(importList, null);
    }

    public void visitClassBody(@NotNull KtClassBody classBody) {
        super.visitClassBody(classBody, null);
    }

    public void visitModifierList(@NotNull KtModifierList list) {
        super.visitModifierList(list, null);
    }

    public void visitAnnotation(@NotNull KtAnnotation annotation) {
        super.visitAnnotation(annotation, null);
    }

    public void visitAnnotationEntry(@NotNull KtAnnotationEntry annotationEntry) {
        super.visitAnnotationEntry(annotationEntry, null);
    }

    public void visitConstructorCalleeExpression(@NotNull KtConstructorCalleeExpression constructorCalleeExpression) {
        super.visitConstructorCalleeExpression(constructorCalleeExpression, null);
    }

    public void visitTypeParameterList(@NotNull KtTypeParameterList list) {
        super.visitTypeParameterList(list, null);
    }

    public void visitTypeParameter(@NotNull KtTypeParameter parameter) {
        super.visitTypeParameter(parameter, null);
    }

    public void visitEnumEntry(@NotNull KtEnumEntry enumEntry) {
        super.visitEnumEntry(enumEntry, null);
    }

    public void visitParameterList(@NotNull KtParameterList list) {
        super.visitParameterList(list, null);
    }

    public void visitParameter(@NotNull KtParameter parameter) {
        super.visitParameter(parameter, null);
    }

    public void visitSuperTypeList(@NotNull KtSuperTypeList list) {
        super.visitSuperTypeList(list, null);
    }

    public void visitSuperTypeListEntry(@NotNull KtSuperTypeListEntry specifier) {
        super.visitSuperTypeListEntry(specifier, null);
    }

    public void visitDelegatedSuperTypeEntry(@NotNull KtDelegatedSuperTypeEntry specifier) {
        super.visitDelegatedSuperTypeEntry(specifier, null);
    }

    public void visitSuperTypeCallEntry(@NotNull KtSuperTypeCallEntry call) {
        super.visitSuperTypeCallEntry(call, null);
    }

    public void visitSuperTypeEntry(@NotNull KtSuperTypeEntry specifier) {
        super.visitSuperTypeEntry(specifier, null);
    }

    public void visitConstructorDelegationCall(@NotNull KtConstructorDelegationCall call) {
        super.visitConstructorDelegationCall(call, null);
    }

    public void visitPropertyDelegate(@NotNull KtPropertyDelegate delegate) {
        super.visitPropertyDelegate(delegate, null);
    }

    public void visitTypeReference(@NotNull KtTypeReference typeReference) {
        super.visitTypeReference(typeReference, null);
    }

    public void visitValueArgumentList(@NotNull KtValueArgumentList list) {
        super.visitValueArgumentList(list, null);
    }

    public void visitArgument(@NotNull KtValueArgument argument) {
        super.visitArgument(argument, null);
    }

    public void visitExpression(@NotNull KtExpression expression) {
        super.visitExpression(expression, null);
    }

    public void visitLoopExpression(@NotNull KtLoopExpression loopExpression) {
        super.visitLoopExpression(loopExpression, null);
    }

    public void visitConstantExpression(@NotNull KtConstantExpression expression) {
        super.visitConstantExpression(expression, null);
    }

    public void visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression) {
        super.visitSimpleNameExpression(expression, null);
    }

    public void visitReferenceExpression(@NotNull KtReferenceExpression expression) {
        super.visitReferenceExpression(expression, null);
    }

    public void visitLabeledExpression(@NotNull KtLabeledExpression expression) {
        super.visitLabeledExpression(expression, null);
    }

    public void visitPrefixExpression(@NotNull KtPrefixExpression expression) {
        super.visitPrefixExpression(expression, null);
    }

    public void visitPostfixExpression(@NotNull KtPostfixExpression expression) {
        super.visitPostfixExpression(expression, null);
    }

    public void visitUnaryExpression(@NotNull KtUnaryExpression expression) {
        super.visitUnaryExpression(expression, null);
    }

    public void visitBinaryExpression(@NotNull KtBinaryExpression expression) {
        super.visitBinaryExpression(expression, null);
    }

    public void visitReturnExpression(@NotNull KtReturnExpression expression) {
        super.visitReturnExpression(expression, null);
    }

    public void visitExpressionWithLabel(@NotNull KtExpressionWithLabel expression) {
        super.visitExpressionWithLabel(expression, null);
    }

    public void visitThrowExpression(@NotNull KtThrowExpression expression) {
        super.visitThrowExpression(expression, null);
    }

    public void visitBreakExpression(@NotNull KtBreakExpression expression) {
        super.visitBreakExpression(expression, null);
    }

    public void visitContinueExpression(@NotNull KtContinueExpression expression) {
        super.visitContinueExpression(expression, null);
    }

    public void visitIfExpression(@NotNull KtIfExpression expression) {
        super.visitIfExpression(expression, null);
    }

    public void visitWhenExpression(@NotNull KtWhenExpression expression) {
        super.visitWhenExpression(expression, null);
    }

    public void visitCollectionLiteralExpression(@NotNull KtCollectionLiteralExpression expression) {
        super.visitCollectionLiteralExpression(expression, null);
    }

    public void visitTryExpression(@NotNull KtTryExpression expression) {
        super.visitTryExpression(expression, null);
    }

    public void visitForExpression(@NotNull KtForExpression expression) {
        super.visitForExpression(expression, null);
    }

    public void visitWhileExpression(@NotNull KtWhileExpression expression) {
        super.visitWhileExpression(expression, null);
    }

    public void visitDoWhileExpression(@NotNull KtDoWhileExpression expression) {
        super.visitDoWhileExpression(expression, null);
    }

    public void visitLambdaExpression(@NotNull KtLambdaExpression lambdaExpression) {
        super.visitLambdaExpression(lambdaExpression, null);
    }

    public void visitAnnotatedExpression(@NotNull KtAnnotatedExpression expression) {
        super.visitAnnotatedExpression(expression, null);
    }

    public void visitCallExpression(@NotNull KtCallExpression expression) {
        super.visitCallExpression(expression, null);
    }

    public void visitArrayAccessExpression(@NotNull KtArrayAccessExpression expression) {
        super.visitArrayAccessExpression(expression, null);
    }

    public void visitQualifiedExpression(@NotNull KtQualifiedExpression expression) {
        super.visitQualifiedExpression(expression, null);
    }

    public void visitDoubleColonExpression(@NotNull KtDoubleColonExpression expression) {
        super.visitDoubleColonExpression(expression, null);
    }

    public void visitCallableReferenceExpression(@NotNull KtCallableReferenceExpression expression) {
        super.visitCallableReferenceExpression(expression, null);
    }

    public void visitClassLiteralExpression(@NotNull KtClassLiteralExpression expression) {
        super.visitClassLiteralExpression(expression, null);
    }

    public void visitDotQualifiedExpression(@NotNull KtDotQualifiedExpression expression) {
        super.visitDotQualifiedExpression(expression, null);
    }

    public void visitSafeQualifiedExpression(@NotNull KtSafeQualifiedExpression expression) {
        super.visitSafeQualifiedExpression(expression, null);
    }

    public void visitObjectLiteralExpression(@NotNull KtObjectLiteralExpression expression) {
        super.visitObjectLiteralExpression(expression, null);
    }

    public void visitBlockExpression(@NotNull KtBlockExpression expression) {
        super.visitBlockExpression(expression, null);
    }

    public void visitCatchSection(@NotNull KtCatchClause catchClause) {
        super.visitCatchSection(catchClause, null);
    }

    public void visitFinallySection(@NotNull KtFinallySection finallySection) {
        super.visitFinallySection(finallySection, null);
    }

    public void visitTypeArgumentList(@NotNull KtTypeArgumentList typeArgumentList) {
        super.visitTypeArgumentList(typeArgumentList, null);
    }

    public void visitThisExpression(@NotNull KtThisExpression expression) {
        super.visitThisExpression(expression, null);
    }

    public void visitSuperExpression(@NotNull KtSuperExpression expression) {
        super.visitSuperExpression(expression, null);
    }

    public void visitParenthesizedExpression(@NotNull KtParenthesizedExpression expression) {
        super.visitParenthesizedExpression(expression, null);
    }

    public void visitInitializerList(@NotNull KtInitializerList list) {
        super.visitInitializerList(list, null);
    }

    public void visitAnonymousInitializer(@NotNull KtAnonymousInitializer initializer) {
        super.visitAnonymousInitializer(initializer, null);
    }

    public void visitScriptInitializer(@NotNull KtScriptInitializer initializer) {
        super.visitScriptInitializer(initializer, null);
    }

    public void visitClassInitializer(@NotNull KtClassInitializer initializer) {
        super.visitClassInitializer(initializer, null);
    }

    public void visitPropertyAccessor(@NotNull KtPropertyAccessor accessor) {
        super.visitPropertyAccessor(accessor, null);
    }

    public void visitTypeConstraintList(@NotNull KtTypeConstraintList list) {
        super.visitTypeConstraintList(list, null);
    }

    public void visitTypeConstraint(@NotNull KtTypeConstraint constraint) {
        super.visitTypeConstraint(constraint, null);
    }

    public void visitUserType(@NotNull KtUserType type) {
        super.visitUserType(type, null);
    }

    public void visitDynamicType(@NotNull KtDynamicType type) {
        super.visitDynamicType(type, null);
    }

    public void visitFunctionType(@NotNull KtFunctionType type) {
        super.visitFunctionType(type, null);
    }

    public void visitSelfType(@NotNull KtSelfType type) {
        super.visitSelfType(type, null);
    }

    public void visitBinaryWithTypeRHSExpression(@NotNull KtBinaryExpressionWithTypeRHS expression) {
        super.visitBinaryWithTypeRHSExpression(expression, null);
    }

    public void visitStringTemplateExpression(@NotNull KtStringTemplateExpression expression) {
        super.visitStringTemplateExpression(expression, null);
    }

    public void visitNamedDeclaration(@NotNull KtNamedDeclaration declaration) {
        super.visitNamedDeclaration(declaration, null);
    }

    public void visitNullableType(@NotNull KtNullableType nullableType) {
        super.visitNullableType(nullableType, null);
    }

    public void visitTypeProjection(@NotNull KtTypeProjection typeProjection) {
        super.visitTypeProjection(typeProjection, null);
    }

    public void visitWhenEntry(@NotNull KtWhenEntry jetWhenEntry) {
        super.visitWhenEntry(jetWhenEntry, null);
    }

    public void visitIsExpression(@NotNull KtIsExpression expression) {
        super.visitIsExpression(expression, null);
    }

    public void visitWhenConditionIsPattern(@NotNull KtWhenConditionIsPattern condition) {
        super.visitWhenConditionIsPattern(condition, null);
    }

    public void visitWhenConditionInRange(@NotNull KtWhenConditionInRange condition) {
        super.visitWhenConditionInRange(condition, null);
    }

    public void visitWhenConditionWithExpression(@NotNull KtWhenConditionWithExpression condition) {
        super.visitWhenConditionWithExpression(condition, null);
    }

    public void visitObjectDeclaration(@NotNull KtObjectDeclaration declaration) {
        super.visitObjectDeclaration(declaration, null);
    }

    public void visitStringTemplateEntry(@NotNull KtStringTemplateEntry entry) {
        super.visitStringTemplateEntry(entry, null);
    }

    public void visitStringTemplateEntryWithExpression(@NotNull KtStringTemplateEntryWithExpression entry) {
        super.visitStringTemplateEntryWithExpression(entry, null);
    }

    public void visitBlockStringTemplateEntry(@NotNull KtBlockStringTemplateEntry entry) {
        super.visitBlockStringTemplateEntry(entry, null);
    }

    public void visitSimpleNameStringTemplateEntry(@NotNull KtSimpleNameStringTemplateEntry entry) {
        super.visitSimpleNameStringTemplateEntry(entry, null);
    }

    public void visitLiteralStringTemplateEntry(@NotNull KtLiteralStringTemplateEntry entry) {
        super.visitLiteralStringTemplateEntry(entry, null);
    }

    public void visitEscapeStringTemplateEntry(@NotNull KtEscapeStringTemplateEntry entry) {
        super.visitEscapeStringTemplateEntry(entry, null);
    }

    public void visitPackageDirective(@NotNull KtPackageDirective directive) {
        super.visitPackageDirective(directive, null);
    }

    // hidden methods
    @Override
    public final Void visitKtElement(@NotNull KtElement element, Void data) {
        visitKtElement(element);
        return null;
    }

    @Override
    public final Void visitDeclaration(@NotNull KtDeclaration dcl, Void data) {
        visitDeclaration(dcl);
        return null;
    }

    @Override
    public final Void visitClass(@NotNull KtClass klass, Void data) {
        visitClass(klass);
        return null;
    }

    @Override
    public final Void visitClassOrObject(@NotNull KtClassOrObject classOrObject, Void data) {
        visitClassOrObject(classOrObject);
        return null;
    }

    @Override
    public final Void visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor, Void data) {
        visitSecondaryConstructor(constructor);
        return null;
    }

    @Override
    public final Void visitPrimaryConstructor(@NotNull KtPrimaryConstructor constructor, Void data) {
        visitPrimaryConstructor(constructor);
        return null;
    }

    @Override
    public final Void visitNamedFunction(@NotNull KtNamedFunction function, Void data) {
        visitNamedFunction(function);
        return null;
    }

    @Override
    public final Void visitProperty(@NotNull KtProperty property, Void data) {
        visitProperty(property);
        return null;
    }

    @Override
    public final Void visitTypeAlias(@NotNull KtTypeAlias typeAlias, Void data) {
        visitTypeAlias(typeAlias);
        return null;
    }

    @Override
    public final Void visitDestructuringDeclaration(@NotNull KtDestructuringDeclaration multiDeclaration, Void data) {
        visitDestructuringDeclaration(multiDeclaration);
        return null;
    }

    @Override
    public final Void visitDestructuringDeclarationEntry(@NotNull KtDestructuringDeclarationEntry multiDeclarationEntry, Void data) {
        visitDestructuringDeclarationEntry(multiDeclarationEntry);
        return null;
    }

    @Override
    public final Void visitKtFile(@NotNull KtFile file, Void data) {
        visitKtFile(file);
        return null;
    }

    @Override
    public final Void visitScript(@NotNull KtScript script, Void data) {
        visitScript(script);
        return null;
    }

    @Override
    public final Void visitImportDirective(@NotNull KtImportDirective importDirective, Void data) {
        visitImportDirective(importDirective);
        return null;
    }

    @Override
    public final Void visitImportList(@NotNull KtImportList importList, Void data) {
        visitImportList(importList);
        return null;
    }

    @Override
    public final Void visitClassBody(@NotNull KtClassBody classBody, Void data) {
        visitClassBody(classBody);
        return null;
    }

    @Override
    public final Void visitModifierList(@NotNull KtModifierList list, Void data) {
        visitModifierList(list);
        return null;
    }

    @Override
    public final Void visitAnnotation(@NotNull KtAnnotation annotation, Void data) {
        visitAnnotation(annotation);
        return null;
    }

    @Override
    public final Void visitAnnotationEntry(@NotNull KtAnnotationEntry annotationEntry, Void data) {
        visitAnnotationEntry(annotationEntry);
        return null;
    }

    @Override
    public final Void visitConstructorCalleeExpression(@NotNull KtConstructorCalleeExpression constructorCalleeExpression, Void data) {
        visitConstructorCalleeExpression(constructorCalleeExpression);
        return null;
    }

    @Override
    public final Void visitTypeParameterList(@NotNull KtTypeParameterList list, Void data) {
        visitTypeParameterList(list);
        return null;
    }

    @Override
    public final Void visitTypeParameter(@NotNull KtTypeParameter parameter, Void data) {
        visitTypeParameter(parameter);
        return null;
    }

    @Override
    public final Void visitEnumEntry(@NotNull KtEnumEntry enumEntry, Void data) {
        visitEnumEntry(enumEntry);
        return null;
    }

    @Override
    public final Void visitParameterList(@NotNull KtParameterList list, Void data) {
        visitParameterList(list);
        return null;
    }

    @Override
    public final Void visitParameter(@NotNull KtParameter parameter, Void data) {
        visitParameter(parameter);
        return null;
    }

    @Override
    public final Void visitSuperTypeList(@NotNull KtSuperTypeList list, Void data) {
        visitSuperTypeList(list);
        return null;
    }

    @Override
    public final Void visitSuperTypeListEntry(@NotNull KtSuperTypeListEntry specifier, Void data) {
        visitSuperTypeListEntry(specifier);
        return null;
    }

    @Override
    public final Void visitDelegatedSuperTypeEntry(
            @NotNull KtDelegatedSuperTypeEntry specifier, Void data
    ) {
        visitDelegatedSuperTypeEntry(specifier);
        return null;
    }

    @Override
    public final Void visitSuperTypeCallEntry(@NotNull KtSuperTypeCallEntry call, Void data) {
        visitSuperTypeCallEntry(call);
        return null;
    }

    @Override
    public final Void visitSuperTypeEntry(@NotNull KtSuperTypeEntry specifier, Void data) {
        visitSuperTypeEntry(specifier);
        return null;
    }

    @Override
    public final Void visitConstructorDelegationCall(@NotNull KtConstructorDelegationCall call, Void data) {
        visitConstructorDelegationCall(call);
        return null;
    }

    @Override
    public final Void visitPropertyDelegate(@NotNull KtPropertyDelegate delegate, Void data) {
        visitPropertyDelegate(delegate);
        return null;
    }

    @Override
    public final Void visitTypeReference(@NotNull KtTypeReference typeReference, Void data) {
        visitTypeReference(typeReference);
        return null;
    }

    @Override
    public final Void visitValueArgumentList(@NotNull KtValueArgumentList list, Void data) {
        visitValueArgumentList(list);
        return null;
    }

    @Override
    public final Void visitArgument(@NotNull KtValueArgument argument, Void data) {
        visitArgument(argument);
        return null;
    }

    @Override
    public final Void visitExpression(@NotNull KtExpression expression, Void data) {
        visitExpression(expression);
        return null;
    }

    @Override
    public final Void visitLoopExpression(@NotNull KtLoopExpression loopExpression, Void data) {
        visitLoopExpression(loopExpression);
        return null;
    }

    @Override
    public final Void visitConstantExpression(@NotNull KtConstantExpression expression, Void data) {
        visitConstantExpression(expression);
        return null;
    }

    @Override
    public final Void visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression, Void data) {
        visitSimpleNameExpression(expression);
        return null;
    }

    @Override
    public final Void visitReferenceExpression(@NotNull KtReferenceExpression expression, Void data) {
        visitReferenceExpression(expression);
        return null;
    }

    @Override
    public final Void visitLabeledExpression(@NotNull KtLabeledExpression expression, Void data) {
        visitLabeledExpression(expression);
        return null;
    }

    @Override
    public final Void visitPrefixExpression(@NotNull KtPrefixExpression expression, Void data) {
        visitPrefixExpression(expression);
        return null;
    }

    @Override
    public final Void visitPostfixExpression(@NotNull KtPostfixExpression expression, Void data) {
        visitPostfixExpression(expression);
        return null;
    }

    @Override
    public final Void visitUnaryExpression(@NotNull KtUnaryExpression expression, Void data) {
        visitUnaryExpression(expression);
        return null;
    }

    @Override
    public final Void visitBinaryExpression(@NotNull KtBinaryExpression expression, Void data) {
        visitBinaryExpression(expression);
        return null;
    }

    @Override
    public final Void visitReturnExpression(@NotNull KtReturnExpression expression, Void data) {
        visitReturnExpression(expression);
        return null;
    }

    @Override
    public final Void visitExpressionWithLabel(@NotNull KtExpressionWithLabel expression, Void data) {
        visitExpressionWithLabel(expression);
        return null;
    }

    @Override
    public final Void visitThrowExpression(@NotNull KtThrowExpression expression, Void data) {
        visitThrowExpression(expression);
        return null;
    }

    @Override
    public final Void visitBreakExpression(@NotNull KtBreakExpression expression, Void data) {
        visitBreakExpression(expression);
        return null;
    }

    @Override
    public final Void visitContinueExpression(@NotNull KtContinueExpression expression, Void data) {
        visitContinueExpression(expression);
        return null;
    }

    @Override
    public final Void visitIfExpression(@NotNull KtIfExpression expression, Void data) {
        visitIfExpression(expression);
        return null;
    }

    @Override
    public final Void visitWhenExpression(@NotNull KtWhenExpression expression, Void data) {
        visitWhenExpression(expression);
        return null;
    }

    @Override
    public final Void visitCollectionLiteralExpression(@NotNull KtCollectionLiteralExpression expression, Void data) {
        visitCollectionLiteralExpression(expression);
        return null;
    }

    @Override
    public final Void visitTryExpression(@NotNull KtTryExpression expression, Void data) {
        visitTryExpression(expression);
        return null;
    }

    @Override
    public final Void visitForExpression(@NotNull KtForExpression expression, Void data) {
        visitForExpression(expression);
        return null;
    }

    @Override
    public final Void visitWhileExpression(@NotNull KtWhileExpression expression, Void data) {
        visitWhileExpression(expression);
        return null;
    }

    @Override
    public final Void visitDoWhileExpression(@NotNull KtDoWhileExpression expression, Void data) {
        visitDoWhileExpression(expression);
        return null;
    }

    @Override
    public final Void visitLambdaExpression(@NotNull KtLambdaExpression expression, Void data) {
        visitLambdaExpression(expression);
        return null;
    }

    @Override
    public final Void visitAnnotatedExpression(@NotNull KtAnnotatedExpression expression, Void data) {
        visitAnnotatedExpression(expression);
        return null;
    }

    @Override
    public final Void visitCallExpression(@NotNull KtCallExpression expression, Void data) {
        visitCallExpression(expression);
        return null;
    }

    @Override
    public final Void visitArrayAccessExpression(@NotNull KtArrayAccessExpression expression, Void data) {
        visitArrayAccessExpression(expression);
        return null;
    }

    @Override
    public final Void visitQualifiedExpression(@NotNull KtQualifiedExpression expression, Void data) {
        visitQualifiedExpression(expression);
        return null;
    }

    @Override
    public final Void visitDoubleColonExpression(@NotNull KtDoubleColonExpression expression, Void data) {
        visitDoubleColonExpression(expression);
        return null;
    }

    @Override
    public final Void visitCallableReferenceExpression(@NotNull KtCallableReferenceExpression expression, Void data) {
        visitCallableReferenceExpression(expression);
        return null;
    }

    @Override
    public final Void visitClassLiteralExpression(@NotNull KtClassLiteralExpression expression, Void data) {
        visitClassLiteralExpression(expression);
        return null;
    }

    @Override
    public final Void visitDotQualifiedExpression(@NotNull KtDotQualifiedExpression expression, Void data) {
        visitDotQualifiedExpression(expression);
        return null;
    }

    @Override
    public final Void visitSafeQualifiedExpression(@NotNull KtSafeQualifiedExpression expression, Void data) {
        visitSafeQualifiedExpression(expression);
        return null;
    }

    @Override
    public final Void visitObjectLiteralExpression(@NotNull KtObjectLiteralExpression expression, Void data) {
        visitObjectLiteralExpression(expression);
        return null;
    }

    @Override
    public final Void visitBlockExpression(@NotNull KtBlockExpression expression, Void data) {
        visitBlockExpression(expression);
        return null;
    }

    @Override
    public final Void visitCatchSection(@NotNull KtCatchClause catchClause, Void data) {
        visitCatchSection(catchClause);
        return null;
    }

    @Override
    public final Void visitFinallySection(@NotNull KtFinallySection finallySection, Void data) {
        visitFinallySection(finallySection);
        return null;
    }

    @Override
    public final Void visitTypeArgumentList(@NotNull KtTypeArgumentList typeArgumentList, Void data) {
        visitTypeArgumentList(typeArgumentList);
        return null;
    }

    @Override
    public final Void visitThisExpression(@NotNull KtThisExpression expression, Void data) {
        visitThisExpression(expression);
        return null;
    }

    @Override
    public final Void visitSuperExpression(@NotNull KtSuperExpression expression, Void data) {
        visitSuperExpression(expression);
        return null;
    }

    @Override
    public final Void visitParenthesizedExpression(@NotNull KtParenthesizedExpression expression, Void data) {
        visitParenthesizedExpression(expression);
        return null;
    }

    @Override
    public final Void visitInitializerList(@NotNull KtInitializerList list, Void data) {
        visitInitializerList(list);
        return null;
    }

    @Override
    public final Void visitAnonymousInitializer(@NotNull KtAnonymousInitializer initializer, Void data) {
        visitAnonymousInitializer(initializer);
        return null;
    }

    @Override
    public final Void visitPropertyAccessor(@NotNull KtPropertyAccessor accessor, Void data) {
        visitPropertyAccessor(accessor);
        return null;
    }

    @Override
    public final Void visitTypeConstraintList(@NotNull KtTypeConstraintList list, Void data) {
        visitTypeConstraintList(list);
        return null;
    }

    @Override
    public final Void visitTypeConstraint(@NotNull KtTypeConstraint constraint, Void data) {
        visitTypeConstraint(constraint);
        return null;
    }

    @Override
    public final Void visitUserType(@NotNull KtUserType type, Void data) {
        visitUserType(type);
        return null;
    }

    @Override
    public Void visitDynamicType(@NotNull KtDynamicType type, Void data) {
        visitDynamicType(type);
        return null;
    }

    @Override
    public final Void visitFunctionType(@NotNull KtFunctionType type, Void data) {
        visitFunctionType(type);
        return null;
    }

    @Override
    public final Void visitSelfType(@NotNull KtSelfType type, Void data) {
        visitSelfType(type);
        return null;
    }

    @Override
    public final Void visitBinaryWithTypeRHSExpression(@NotNull KtBinaryExpressionWithTypeRHS expression, Void data) {
        visitBinaryWithTypeRHSExpression(expression);
        return null;
    }

    @Override
    public final Void visitStringTemplateExpression(@NotNull KtStringTemplateExpression expression, Void data) {
        visitStringTemplateExpression(expression);
        return null;
    }

    @Override
    public final Void visitNamedDeclaration(@NotNull KtNamedDeclaration declaration, Void data) {
        visitNamedDeclaration(declaration);
        return null;
    }

    @Override
    public final Void visitNullableType(@NotNull KtNullableType nullableType, Void data) {
        visitNullableType(nullableType);
        return null;
    }

    @Override
    public final Void visitTypeProjection(@NotNull KtTypeProjection typeProjection, Void data) {
        visitTypeProjection(typeProjection);
        return null;
    }

    @Override
    public final Void visitWhenEntry(@NotNull KtWhenEntry jetWhenEntry, Void data) {
        visitWhenEntry(jetWhenEntry);
        return null;
    }

    @Override
    public final Void visitIsExpression(@NotNull KtIsExpression expression, Void data) {
        visitIsExpression(expression);
        return null;
    }

    @Override
    public final Void visitWhenConditionIsPattern(@NotNull KtWhenConditionIsPattern condition, Void data) {
        visitWhenConditionIsPattern(condition);
        return null;
    }

    @Override
    public final Void visitWhenConditionInRange(@NotNull KtWhenConditionInRange condition, Void data) {
        visitWhenConditionInRange(condition);
        return null;
    }

    @Override
    public final Void visitWhenConditionWithExpression(@NotNull KtWhenConditionWithExpression condition, Void data) {
        visitWhenConditionWithExpression(condition);
        return null;
    }

    @Override
    public final Void visitObjectDeclaration(@NotNull KtObjectDeclaration declaration, Void data) {
        visitObjectDeclaration(declaration);
        return null;
    }

    @Override
    public final Void visitStringTemplateEntry(@NotNull KtStringTemplateEntry entry, Void data) {
        visitStringTemplateEntry(entry);
        return null;
    }

    @Override
    public final Void visitStringTemplateEntryWithExpression(@NotNull KtStringTemplateEntryWithExpression entry, Void data) {
        visitStringTemplateEntryWithExpression(entry);
        return null;
    }

    @Override
    public final Void visitBlockStringTemplateEntry(@NotNull KtBlockStringTemplateEntry entry, Void data) {
        visitBlockStringTemplateEntry(entry);
        return null;
    }

    @Override
    public final Void visitSimpleNameStringTemplateEntry(@NotNull KtSimpleNameStringTemplateEntry entry, Void data) {
        visitSimpleNameStringTemplateEntry(entry);
        return null;
    }

    @Override
    public final Void visitLiteralStringTemplateEntry(@NotNull KtLiteralStringTemplateEntry entry, Void data) {
        visitLiteralStringTemplateEntry(entry);
        return null;
    }

    @Override
    public final Void visitEscapeStringTemplateEntry(@NotNull KtEscapeStringTemplateEntry entry, Void data) {
        visitEscapeStringTemplateEntry(entry);
        return null;
    }

    @Override
    public Void visitPackageDirective(@NotNull KtPackageDirective directive, Void data) {
        visitPackageDirective(directive);
        return null;
    }

    @Override
    public Void visitScriptInitializer(@NotNull KtScriptInitializer initializer, Void data) {
        visitScriptInitializer(initializer);
        return null;
    }

    @Override
    public Void visitClassInitializer(@NotNull KtClassInitializer initializer, Void data) {
        visitClassInitializer(initializer);
        return null;
    }
}
