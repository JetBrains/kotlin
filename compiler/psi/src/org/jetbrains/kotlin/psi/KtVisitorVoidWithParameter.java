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

public class KtVisitorVoidWithParameter<P> extends KtVisitor<Void, P> {

    // methods with parameter

    public void visitJetElementVoid(@NotNull KtElement element, P data) {
        super.visitKtElement(element, data);
    }

    public void visitDeclarationVoid(@NotNull KtDeclaration dcl, P data) {
        super.visitDeclaration(dcl, data);
    }

    public void visitClassVoid(@NotNull KtClass klass, P data) {
        super.visitClass(klass, data);
    }

    public void visitSecondaryConstructorVoid(@NotNull KtSecondaryConstructor constructor, P data) {
        super.visitSecondaryConstructor(constructor, data);
    }

    public void visitPrimaryConstructorVoid(@NotNull KtPrimaryConstructor constructor, P data) {
        super.visitPrimaryConstructor(constructor, data);
    }

    public void visitNamedFunctionVoid(@NotNull KtNamedFunction function, P data) {
        super.visitNamedFunction(function, data);
    }

    public void visitPropertyVoid(@NotNull KtProperty property, P data) {
        super.visitProperty(property, data);
    }

    public void visitDestructuringDeclarationVoid(@NotNull KtDestructuringDeclaration multiDeclaration, P data) {
        super.visitDestructuringDeclaration(multiDeclaration, data);
    }

    public void visitDestructuringDeclarationEntryVoid(@NotNull KtDestructuringDeclarationEntry multiDeclarationEntry, P data) {
        super.visitDestructuringDeclarationEntry(multiDeclarationEntry, data);
    }

    public void visitJetFileVoid(@NotNull KtFile file, P data) {
        super.visitKtFile(file, data);
    }

    public void visitScriptVoid(@NotNull KtScript script, P data) {
        super.visitScript(script, data);
    }

    public void visitImportDirectiveVoid(@NotNull KtImportDirective importDirective, P data) {
        super.visitImportDirective(importDirective, data);
    }

    public void visitImportAliasVoid(@NotNull KtImportAlias importAlias, P data) {
        super.visitImportAlias(importAlias, data);
    }

    public void visitImportListVoid(@NotNull KtImportList importList, P data) {
        super.visitImportList(importList, data);
    }

    public void visitClassBodyVoid(@NotNull KtClassBody classBody, P data) {
        super.visitClassBody(classBody, data);
    }

    public void visitModifierListVoid(@NotNull KtModifierList list, P data) {
        super.visitModifierList(list, data);
    }

    public void visitAnnotationVoid(@NotNull KtAnnotation annotation, P data) {
        super.visitAnnotation(annotation, data);
    }

    public void visitAnnotationEntryVoid(@NotNull KtAnnotationEntry annotationEntry, P data) {
        super.visitAnnotationEntry(annotationEntry, data);
    }

    public void visitConstructorCalleeExpressionVoid(@NotNull KtConstructorCalleeExpression constructorCalleeExpression, P data) {
        super.visitConstructorCalleeExpression(constructorCalleeExpression, data);
    }

    public void visitTypeParameterListVoid(@NotNull KtTypeParameterList list, P data) {
        super.visitTypeParameterList(list, data);
    }

    public void visitTypeParameterVoid(@NotNull KtTypeParameter parameter, P data) {
        super.visitTypeParameter(parameter, data);
    }

    public void visitEnumEntryVoid(@NotNull KtEnumEntry enumEntry, P data) {
        super.visitEnumEntry(enumEntry, data);
    }

    public void visitParameterListVoid(@NotNull KtParameterList list, P data) {
        super.visitParameterList(list, data);
    }

    public void visitParameterVoid(@NotNull KtParameter parameter, P data) {
        super.visitParameter(parameter, data);
    }

    public void visitSuperTypeListVoid(@NotNull KtSuperTypeList list, P data) {
        super.visitSuperTypeList(list, data);
    }

    public void visitSuperTypeListEntryVoid(@NotNull KtSuperTypeListEntry specifier, P data) {
        super.visitSuperTypeListEntry(specifier, data);
    }

    public void visitDelegatedSuperTypeEntryVoid(@NotNull KtDelegatedSuperTypeEntry specifier, P data) {
        super.visitDelegatedSuperTypeEntry(specifier, data);
    }

    public void visitSuperTypeCallEntryVoid(@NotNull KtSuperTypeCallEntry call, P data) {
        super.visitSuperTypeCallEntry(call, data);
    }

    public void visitSuperTypeEntryVoid(@NotNull KtSuperTypeEntry specifier, P data) {
        super.visitSuperTypeEntry(specifier, data);
    }

    public void visitConstructorDelegationCallVoid(@NotNull KtConstructorDelegationCall call, P data) {
        super.visitConstructorDelegationCall(call, data);
    }

    public void visitPropertyDelegateVoid(@NotNull KtPropertyDelegate delegate, P data) {
        super.visitPropertyDelegate(delegate, data);
    }

    public void visitTypeReferenceVoid(@NotNull KtTypeReference typeReference, P data) {
        super.visitTypeReference(typeReference, data);
    }

    public void visitValueArgumentListVoid(@NotNull KtValueArgumentList list, P data) {
        super.visitValueArgumentList(list, data);
    }

    public void visitArgumentVoid(@NotNull KtValueArgument argument, P data) {
        super.visitArgument(argument, data);
    }

    public void visitExpressionVoid(@NotNull KtExpression expression, P data) {
        super.visitExpression(expression, data);
    }

    public void visitLoopExpressionVoid(@NotNull KtLoopExpression loopExpression, P data) {
        super.visitLoopExpression(loopExpression, data);
    }

    public void visitConstantExpressionVoid(@NotNull KtConstantExpression expression, P data) {
        super.visitConstantExpression(expression, data);
    }

    public void visitSimpleNameExpressionVoid(@NotNull KtSimpleNameExpression expression, P data) {
        super.visitSimpleNameExpression(expression, data);
    }

    public void visitReferenceExpressionVoid(@NotNull KtReferenceExpression expression, P data) {
        super.visitReferenceExpression(expression, data);
    }

    public void visitLabeledExpressionVoid(@NotNull KtLabeledExpression expression, P data) {
        super.visitLabeledExpression(expression, data);
    }

    public void visitPrefixExpressionVoid(@NotNull KtPrefixExpression expression, P data) {
        super.visitPrefixExpression(expression, data);
    }

    public void visitPostfixExpressionVoid(@NotNull KtPostfixExpression expression, P data) {
        super.visitPostfixExpression(expression, data);
    }

    public void visitUnaryExpressionVoid(@NotNull KtUnaryExpression expression, P data) {
        super.visitUnaryExpression(expression, data);
    }

    public void visitBinaryExpressionVoid(@NotNull KtBinaryExpression expression, P data) {
        super.visitBinaryExpression(expression, data);
    }

    public void visitReturnExpressionVoid(@NotNull KtReturnExpression expression, P data) {
        super.visitReturnExpression(expression, data);
    }

    public void visitExpressionWithLabelVoid(@NotNull KtExpressionWithLabel expression, P data) {
        super.visitExpressionWithLabel(expression, data);
    }

    public void visitThrowExpressionVoid(@NotNull KtThrowExpression expression, P data) {
        super.visitThrowExpression(expression, data);
    }

    public void visitBreakExpressionVoid(@NotNull KtBreakExpression expression, P data) {
        super.visitBreakExpression(expression, data);
    }

    public void visitContinueExpressionVoid(@NotNull KtContinueExpression expression, P data) {
        super.visitContinueExpression(expression, data);
    }

    public void visitIfExpressionVoid(@NotNull KtIfExpression expression, P data) {
        super.visitIfExpression(expression, data);
    }

    public void visitWhenExpressionVoid(@NotNull KtWhenExpression expression, P data) {
        super.visitWhenExpression(expression, data);
    }

    public void visitTryExpressionVoid(@NotNull KtTryExpression expression, P data) {
        super.visitTryExpression(expression, data);
    }

    public void visitForExpressionVoid(@NotNull KtForExpression expression, P data) {
        super.visitForExpression(expression, data);
    }

    public void visitWhileExpressionVoid(@NotNull KtWhileExpression expression, P data) {
        super.visitWhileExpression(expression, data);
    }

    public void visitDoWhileExpressionVoid(@NotNull KtDoWhileExpression expression, P data) {
        super.visitDoWhileExpression(expression, data);
    }

    public void visitLambdaExpressionVoid(@NotNull KtLambdaExpression expression, P data) {
        super.visitLambdaExpression(expression, data);
    }

    public void visitAnnotatedExpressionVoid(@NotNull KtAnnotatedExpression expression, P data) {
        super.visitAnnotatedExpression(expression, data);
    }

    public void visitCallExpressionVoid(@NotNull KtCallExpression expression, P data) {
        super.visitCallExpression(expression, data);
    }

    public void visitArrayAccessExpressionVoid(@NotNull KtArrayAccessExpression expression, P data) {
        super.visitArrayAccessExpression(expression, data);
    }

    public void visitQualifiedExpressionVoid(@NotNull KtQualifiedExpression expression, P data) {
        super.visitQualifiedExpression(expression, data);
    }

    public void visitDoubleColonExpressionVoid(@NotNull KtDoubleColonExpression expression, P data) {
        super.visitDoubleColonExpression(expression, data);
    }

    public void visitCallableReferenceExpressionVoid(@NotNull KtCallableReferenceExpression expression, P data) {
        super.visitCallableReferenceExpression(expression, data);
    }

    public void visitClassLiteralExpressionVoid(@NotNull KtClassLiteralExpression expression, P data) {
        super.visitClassLiteralExpression(expression, data);
    }

    public void visitDotQualifiedExpressionVoid(@NotNull KtDotQualifiedExpression expression, P data) {
        super.visitDotQualifiedExpression(expression, data);
    }

    public void visitSafeQualifiedExpressionVoid(@NotNull KtSafeQualifiedExpression expression, P data) {
        super.visitSafeQualifiedExpression(expression, data);
    }

    public void visitObjectLiteralExpressionVoid(@NotNull KtObjectLiteralExpression expression, P data) {
        super.visitObjectLiteralExpression(expression, data);
    }

    public void visitBlockExpressionVoid(@NotNull KtBlockExpression expression, P data) {
        super.visitBlockExpression(expression, data);
    }

    public void visitCatchSectionVoid(@NotNull KtCatchClause catchClause, P data) {
        super.visitCatchSection(catchClause, data);
    }

    public void visitFinallySectionVoid(@NotNull KtFinallySection finallySection, P data) {
        super.visitFinallySection(finallySection, data);
    }

    public void visitTypeArgumentListVoid(@NotNull KtTypeArgumentList typeArgumentList, P data) {
        super.visitTypeArgumentList(typeArgumentList, data);
    }

    public void visitThisExpressionVoid(@NotNull KtThisExpression expression, P data) {
        super.visitThisExpression(expression, data);
    }

    public void visitSuperExpressionVoid(@NotNull KtSuperExpression expression, P data) {
        super.visitSuperExpression(expression, data);
    }

    public void visitParenthesizedExpressionVoid(@NotNull KtParenthesizedExpression expression, P data) {
        super.visitParenthesizedExpression(expression, data);
    }

    public void visitInitializerListVoid(@NotNull KtInitializerList list, P data) {
        super.visitInitializerList(list, data);
    }

    public void visitAnonymousInitializerVoid(@NotNull KtAnonymousInitializer initializer, P data) {
        super.visitAnonymousInitializer(initializer, data);
    }

    public void visitPropertyAccessorVoid(@NotNull KtPropertyAccessor accessor, P data) {
        super.visitPropertyAccessor(accessor, data);
    }

    public void visitTypeConstraintListVoid(@NotNull KtTypeConstraintList list, P data) {
        super.visitTypeConstraintList(list, data);
    }

    public void visitTypeConstraintVoid(@NotNull KtTypeConstraint constraint, P data) {
        super.visitTypeConstraint(constraint, data);
    }

    public void visitUserTypeVoid(@NotNull KtUserType type, P data) {
        super.visitUserType(type, data);
    }

    public void visitDynamicTypeVoid(@NotNull KtDynamicType type, P data) {
        super.visitDynamicType(type, data);
    }

    public void visitFunctionTypeVoid(@NotNull KtFunctionType type, P data) {
        super.visitFunctionType(type, data);
    }

    public void visitSelfTypeVoid(@NotNull KtSelfType type, P data) {
        super.visitSelfType(type, data);
    }

    public void visitBinaryWithTypeRHSExpressionVoid(@NotNull KtBinaryExpressionWithTypeRHS expression, P data) {
        super.visitBinaryWithTypeRHSExpression(expression, data);
    }

    public void visitStringTemplateExpressionVoid(@NotNull KtStringTemplateExpression expression, P data) {
        super.visitStringTemplateExpression(expression, data);
    }

    public void visitNamedDeclarationVoid(@NotNull KtNamedDeclaration declaration, P data) {
        super.visitNamedDeclaration(declaration, data);
    }

    public void visitNullableTypeVoid(@NotNull KtNullableType nullableType, P data) {
        super.visitNullableType(nullableType, data);
    }

    public void visitTypeProjectionVoid(@NotNull KtTypeProjection typeProjection, P data) {
        super.visitTypeProjection(typeProjection, data);
    }

    public void visitWhenEntryVoid(@NotNull KtWhenEntry jetWhenEntry, P data) {
        super.visitWhenEntry(jetWhenEntry, data);
    }

    public void visitCollectionLiteralExpressionVoid(@NotNull KtCollectionLiteralExpression expression, P data) {
        super.visitCollectionLiteralExpression(expression, data);
    }

    public void visitIsExpressionVoid(@NotNull KtIsExpression expression, P data) {
        super.visitIsExpression(expression, data);
    }

    public void visitWhenConditionIsPatternVoid(@NotNull KtWhenConditionIsPattern condition, P data) {
        super.visitWhenConditionIsPattern(condition, data);
    }

    public void visitWhenConditionInRangeVoid(@NotNull KtWhenConditionInRange condition, P data) {
        super.visitWhenConditionInRange(condition, data);
    }

    public void visitWhenConditionWithExpressionVoid(@NotNull KtWhenConditionWithExpression condition, P data) {
        super.visitWhenConditionWithExpression(condition, data);
    }

    public void visitObjectDeclarationVoid(@NotNull KtObjectDeclaration declaration, P data) {
        super.visitObjectDeclaration(declaration, data);
    }

    public void visitStringTemplateEntryVoid(@NotNull KtStringTemplateEntry entry, P data) {
        super.visitStringTemplateEntry(entry, data);
    }

    public void visitStringTemplateEntryWithExpressionVoid(@NotNull KtStringTemplateEntryWithExpression entry, P data) {
        super.visitStringTemplateEntryWithExpression(entry, data);
    }

    public void visitBlockStringTemplateEntryVoid(@NotNull KtBlockStringTemplateEntry entry, P data) {
        super.visitBlockStringTemplateEntry(entry, data);
    }

    public void visitSimpleNameStringTemplateEntryVoid(@NotNull KtSimpleNameStringTemplateEntry entry, P data) {
        super.visitSimpleNameStringTemplateEntry(entry, data);
    }

    public void visitLiteralStringTemplateEntryVoid(@NotNull KtLiteralStringTemplateEntry entry, P data) {
        super.visitLiteralStringTemplateEntry(entry, data);
    }

    public void visitEscapeStringTemplateEntryVoid(@NotNull KtEscapeStringTemplateEntry entry, P data) {
        super.visitEscapeStringTemplateEntry(entry, data);
    }

    // hidden methods
    @Override
    public final Void visitKtElement(@NotNull KtElement element, P data) {
        visitJetElementVoid(element, data);
    	return null;
    }

    @Override
    public final Void visitDeclaration(@NotNull KtDeclaration dcl, P data) {
        visitDeclarationVoid(dcl, data);
    	return null;
    }

    @Override
    public final Void visitClass(@NotNull KtClass klass, P data) {
        visitClassVoid(klass, data);
    	return null;
    }

    @Override
    public Void visitPrimaryConstructor(@NotNull KtPrimaryConstructor constructor, P data) {
        visitPrimaryConstructorVoid(constructor, data);
        return null;
    }

    @Override
    public final Void visitNamedFunction(@NotNull KtNamedFunction function, P data) {
        visitNamedFunctionVoid(function, data);
    	return null;
    }

    @Override
    public final Void visitProperty(@NotNull KtProperty property, P data) {
        visitPropertyVoid(property, data);
    	return null;
    }

    @Override
    public final Void visitDestructuringDeclaration(@NotNull KtDestructuringDeclaration multiDeclaration, P data) {
        visitDestructuringDeclarationVoid(multiDeclaration, data);
    	return null;
    }

    @Override
    public final Void visitDestructuringDeclarationEntry(@NotNull KtDestructuringDeclarationEntry multiDeclarationEntry, P data) {
        visitDestructuringDeclarationEntryVoid(multiDeclarationEntry, data);
    	return null;
    }

    @Override
    public final Void visitKtFile(@NotNull KtFile file, P data) {
        visitJetFileVoid(file, data);
        return null;
    }

    @Override
    public final Void visitScript(@NotNull KtScript script, P data) {
        visitScriptVoid(script, data);
        return null;
    }

    @Override
    public final Void visitImportDirective(@NotNull KtImportDirective importDirective, P data) {
        visitImportDirectiveVoid(importDirective, data);
    	return null;
    }

    @Override
    public final Void visitImportList(@NotNull KtImportList importList, P data) {
        visitImportListVoid(importList, data);
    	return null;
    }

    @Override
    public final Void visitClassBody(@NotNull KtClassBody classBody, P data) {
        visitClassBodyVoid(classBody, data);
    	return null;
    }

    @Override
    public final Void visitModifierList(@NotNull KtModifierList list, P data) {
        visitModifierListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitAnnotation(@NotNull KtAnnotation annotation, P data) {
        visitAnnotationVoid(annotation, data);
    	return null;
    }

    @Override
    public final Void visitAnnotationEntry(@NotNull KtAnnotationEntry annotationEntry, P data) {
        visitAnnotationEntryVoid(annotationEntry, data);
    	return null;
    }

    @Override
    public final Void visitTypeParameterList(@NotNull KtTypeParameterList list, P data) {
        visitTypeParameterListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitTypeParameter(@NotNull KtTypeParameter parameter, P data) {
        visitTypeParameterVoid(parameter, data);
    	return null;
    }

    @Override
    public final Void visitEnumEntry(@NotNull KtEnumEntry enumEntry, P data) {
        visitEnumEntryVoid(enumEntry, data);
    	return null;
    }

    @Override
    public final Void visitParameterList(@NotNull KtParameterList list, P data) {
        visitParameterListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitParameter(@NotNull KtParameter parameter, P data) {
        visitParameterVoid(parameter, data);
    	return null;
    }

    @Override
    public final Void visitSuperTypeList(@NotNull KtSuperTypeList list, P data) {
        visitSuperTypeListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitSuperTypeListEntry(@NotNull KtSuperTypeListEntry specifier, P data) {
        visitSuperTypeListEntryVoid(specifier, data);
    	return null;
    }

    @Override
    public final Void visitDelegatedSuperTypeEntry(
            @NotNull KtDelegatedSuperTypeEntry specifier, P data
    ) {
        visitDelegatedSuperTypeEntryVoid(specifier, data);
    	return null;
    }

    @Override
    public final Void visitSuperTypeCallEntry(@NotNull KtSuperTypeCallEntry call, P data) {
        visitSuperTypeCallEntryVoid(call, data);
    	return null;
    }

    @Override
    public final Void visitSuperTypeEntry(@NotNull KtSuperTypeEntry specifier, P data) {
        visitSuperTypeEntryVoid(specifier, data);
    	return null;
    }

    @Override
    public final Void visitConstructorDelegationCall(@NotNull KtConstructorDelegationCall call, P data) {
        visitConstructorDelegationCallVoid(call, data);
        return null;
    }

    @Override
    public final Void visitPropertyDelegate(@NotNull KtPropertyDelegate delegate, P data) {
        visitPropertyDelegateVoid(delegate, data);
    	return null;
    }

    @Override
    public final Void visitTypeReference(@NotNull KtTypeReference typeReference, P data) {
        visitTypeReferenceVoid(typeReference, data);
    	return null;
    }

    @Override
    public final Void visitValueArgumentList(@NotNull KtValueArgumentList list, P data) {
        visitValueArgumentListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitArgument(@NotNull KtValueArgument argument, P data) {
        visitArgumentVoid(argument, data);
    	return null;
    }

    @Override
    public final Void visitExpression(@NotNull KtExpression expression, P data) {
        visitExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitLoopExpression(@NotNull KtLoopExpression loopExpression, P data) {
        visitLoopExpressionVoid(loopExpression, data);
    	return null;
    }

    @Override
    public final Void visitConstantExpression(@NotNull KtConstantExpression expression, P data) {
        visitConstantExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression, P data) {
        visitSimpleNameExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitReferenceExpression(@NotNull KtReferenceExpression expression, P data) {
        visitReferenceExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitLabeledExpression(@NotNull KtLabeledExpression expression, P data) {
        visitLabeledExpressionVoid(expression, data);
        return null;
    }

    @Override
    public final Void visitPrefixExpression(@NotNull KtPrefixExpression expression, P data) {
        visitPrefixExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitPostfixExpression(@NotNull KtPostfixExpression expression, P data) {
        visitPostfixExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitUnaryExpression(@NotNull KtUnaryExpression expression, P data) {
        visitUnaryExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitBinaryExpression(@NotNull KtBinaryExpression expression, P data) {
        visitBinaryExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitReturnExpression(@NotNull KtReturnExpression expression, P data) {
        visitReturnExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitExpressionWithLabel(@NotNull KtExpressionWithLabel expression, P data) {
        visitExpressionWithLabelVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitThrowExpression(@NotNull KtThrowExpression expression, P data) {
        visitThrowExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitBreakExpression(@NotNull KtBreakExpression expression, P data) {
        visitBreakExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitContinueExpression(@NotNull KtContinueExpression expression, P data) {
        visitContinueExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitIfExpression(@NotNull KtIfExpression expression, P data) {
        visitIfExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitWhenExpression(@NotNull KtWhenExpression expression, P data) {
        visitWhenExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public Void visitCollectionLiteralExpression(@NotNull KtCollectionLiteralExpression expression, P data) {
        visitCollectionLiteralExpressionVoid(expression, data);
        return null;
    }

    @Override
    public final Void visitTryExpression(@NotNull KtTryExpression expression, P data) {
        visitTryExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitForExpression(@NotNull KtForExpression expression, P data) {
        visitForExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitWhileExpression(@NotNull KtWhileExpression expression, P data) {
        visitWhileExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitDoWhileExpression(@NotNull KtDoWhileExpression expression, P data) {
        visitDoWhileExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitLambdaExpression(@NotNull KtLambdaExpression expression, P data) {
        visitLambdaExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitAnnotatedExpression(@NotNull KtAnnotatedExpression expression, P data) {
        visitAnnotatedExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitCallExpression(@NotNull KtCallExpression expression, P data) {
        visitCallExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitArrayAccessExpression(@NotNull KtArrayAccessExpression expression, P data) {
        visitArrayAccessExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitQualifiedExpression(@NotNull KtQualifiedExpression expression, P data) {
        visitQualifiedExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitDoubleColonExpression(@NotNull KtDoubleColonExpression expression, P data) {
        visitDoubleColonExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitCallableReferenceExpression(@NotNull KtCallableReferenceExpression expression, P data) {
        visitCallableReferenceExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitClassLiteralExpression(@NotNull KtClassLiteralExpression expression, P data) {
        visitClassLiteralExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitDotQualifiedExpression(@NotNull KtDotQualifiedExpression expression, P data) {
        visitDotQualifiedExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitSafeQualifiedExpression(@NotNull KtSafeQualifiedExpression expression, P data) {
        visitSafeQualifiedExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitObjectLiteralExpression(@NotNull KtObjectLiteralExpression expression, P data) {
        visitObjectLiteralExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitBlockExpression(@NotNull KtBlockExpression expression, P data) {
        visitBlockExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitCatchSection(@NotNull KtCatchClause catchClause, P data) {
        visitCatchSectionVoid(catchClause, data);
    	return null;
    }

    @Override
    public final Void visitFinallySection(@NotNull KtFinallySection finallySection, P data) {
        visitFinallySectionVoid(finallySection, data);
    	return null;
    }

    @Override
    public final Void visitTypeArgumentList(@NotNull KtTypeArgumentList typeArgumentList, P data) {
        visitTypeArgumentListVoid(typeArgumentList, data);
    	return null;
    }

    @Override
    public final Void visitThisExpression(@NotNull KtThisExpression expression, P data) {
        visitThisExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitSuperExpression(@NotNull KtSuperExpression expression, P data) {
        visitSuperExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitParenthesizedExpression(@NotNull KtParenthesizedExpression expression, P data) {
        visitParenthesizedExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitInitializerList(@NotNull KtInitializerList list, P data) {
        visitInitializerListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitAnonymousInitializer(@NotNull KtAnonymousInitializer initializer, P data) {
        visitAnonymousInitializerVoid(initializer, data);
    	return null;
    }

    @Override
    public final Void visitPropertyAccessor(@NotNull KtPropertyAccessor accessor, P data) {
        visitPropertyAccessorVoid(accessor, data);
    	return null;
    }

    @Override
    public final Void visitTypeConstraintList(@NotNull KtTypeConstraintList list, P data) {
        visitTypeConstraintListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitTypeConstraint(@NotNull KtTypeConstraint constraint, P data) {
        visitTypeConstraintVoid(constraint, data);
    	return null;
    }

    @Override
    public final Void visitUserType(@NotNull KtUserType type, P data) {
        visitUserTypeVoid(type, data);
    	return null;
    }

    @Override
    public Void visitDynamicType(@NotNull KtDynamicType type, P data) {
        visitDynamicTypeVoid(type, data);
        return null;
    }

    @Override
    public final Void visitFunctionType(@NotNull KtFunctionType type, P data) {
        visitFunctionTypeVoid(type, data);
    	return null;
    }

    @Override
    public final Void visitSelfType(@NotNull KtSelfType type, P data) {
        visitSelfTypeVoid(type, data);
    	return null;
    }

    @Override
    public final Void visitBinaryWithTypeRHSExpression(@NotNull KtBinaryExpressionWithTypeRHS expression, P data) {
        visitBinaryWithTypeRHSExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitStringTemplateExpression(@NotNull KtStringTemplateExpression expression, P data) {
        visitStringTemplateExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitNamedDeclaration(@NotNull KtNamedDeclaration declaration, P data) {
        visitNamedDeclarationVoid(declaration, data);
    	return null;
    }

    @Override
    public final Void visitNullableType(@NotNull KtNullableType nullableType, P data) {
        visitNullableTypeVoid(nullableType, data);
    	return null;
    }

    @Override
    public final Void visitTypeProjection(@NotNull KtTypeProjection typeProjection, P data) {
        visitTypeProjectionVoid(typeProjection, data);
    	return null;
    }

    @Override
    public final Void visitWhenEntry(@NotNull KtWhenEntry jetWhenEntry, P data) {
        visitWhenEntryVoid(jetWhenEntry, data);
    	return null;
    }

    @Override
    public final Void visitIsExpression(@NotNull KtIsExpression expression, P data) {
        visitIsExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitWhenConditionIsPattern(@NotNull KtWhenConditionIsPattern condition, P data) {
        visitWhenConditionIsPatternVoid(condition, data);
    	return null;
    }

    @Override
    public final Void visitWhenConditionInRange(@NotNull KtWhenConditionInRange condition, P data) {
        visitWhenConditionInRangeVoid(condition, data);
    	return null;
    }

    @Override
    public final Void visitWhenConditionWithExpression(@NotNull KtWhenConditionWithExpression condition, P data) {
        visitWhenConditionWithExpressionVoid(condition, data);
    	return null;
    }

    @Override
    public final Void visitObjectDeclaration(@NotNull KtObjectDeclaration declaration, P data) {
        visitObjectDeclarationVoid(declaration, data);
    	return null;
    }

    @Override
    public final Void visitStringTemplateEntry(@NotNull KtStringTemplateEntry entry, P data) {
        visitStringTemplateEntryVoid(entry, data);
    	return null;
    }

    @Override
    public final Void visitStringTemplateEntryWithExpression(@NotNull KtStringTemplateEntryWithExpression entry, P data) {
        visitStringTemplateEntryWithExpressionVoid(entry, data);
    	return null;
    }

    @Override
    public final Void visitBlockStringTemplateEntry(@NotNull KtBlockStringTemplateEntry entry, P data) {
        visitBlockStringTemplateEntryVoid(entry, data);
    	return null;
    }

    @Override
    public final Void visitSimpleNameStringTemplateEntry(@NotNull KtSimpleNameStringTemplateEntry entry, P data) {
        visitSimpleNameStringTemplateEntryVoid(entry, data);
    	return null;
    }

    @Override
    public final Void visitLiteralStringTemplateEntry(@NotNull KtLiteralStringTemplateEntry entry, P data) {
        visitLiteralStringTemplateEntryVoid(entry, data);
    	return null;
    }

    @Override
    public final Void visitEscapeStringTemplateEntry(@NotNull KtEscapeStringTemplateEntry entry, P data) {
        visitEscapeStringTemplateEntryVoid(entry, data);
        return null;
    }
}
