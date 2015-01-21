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

import org.jetbrains.annotations.NotNull;

public class JetVisitorVoidWithParameter<P> extends JetVisitor<Void, P> {

    // methods with parameter

    public void visitJetElementVoid(@NotNull JetElement element, P data) {
        super.visitJetElement(element, data);
    }

    public void visitDeclarationVoid(@NotNull JetDeclaration dcl, P data) {
        super.visitDeclaration(dcl, data);
    }

    public void visitClassVoid(@NotNull JetClass klass, P data) {
        super.visitClass(klass, data);
    }

    public void visitSecondaryConstructorVoid(@NotNull JetSecondaryConstructor constructor, P data) {
        super.visitSecondaryConstructor(constructor, data);
    }

    public void visitNamedFunctionVoid(@NotNull JetNamedFunction function, P data) {
        super.visitNamedFunction(function, data);
    }

    public void visitPropertyVoid(@NotNull JetProperty property, P data) {
        super.visitProperty(property, data);
    }

    public void visitMultiDeclarationVoid(@NotNull JetMultiDeclaration multiDeclaration, P data) {
        super.visitMultiDeclaration(multiDeclaration, data);
    }

    public void visitMultiDeclarationEntryVoid(@NotNull JetMultiDeclarationEntry multiDeclarationEntry, P data) {
        super.visitMultiDeclarationEntry(multiDeclarationEntry, data);
    }

    public void visitTypedefVoid(@NotNull JetTypedef typedef, P data) {
        super.visitTypedef(typedef, data);
    }

    public void visitJetFileVoid(@NotNull JetFile file, P data) {
        super.visitJetFile(file, data);
    }

    public void visitScriptVoid(@NotNull JetScript script, P data) {
        super.visitScript(script, data);
    }

    public void visitImportDirectiveVoid(@NotNull JetImportDirective importDirective, P data) {
        super.visitImportDirective(importDirective, data);
    }

    public void visitImportListVoid(@NotNull JetImportList importList, P data) {
        super.visitImportList(importList, data);
    }

    public void visitClassBodyVoid(@NotNull JetClassBody classBody, P data) {
        super.visitClassBody(classBody, data);
    }

    public void visitModifierListVoid(@NotNull JetModifierList list, P data) {
        super.visitModifierList(list, data);
    }

    public void visitAnnotationVoid(@NotNull JetAnnotation annotation, P data) {
        super.visitAnnotation(annotation, data);
    }

    public void visitAnnotationEntryVoid(@NotNull JetAnnotationEntry annotationEntry, P data) {
        super.visitAnnotationEntry(annotationEntry, data);
    }

    public void visitConstructorCalleeExpressionVoid(@NotNull JetConstructorCalleeExpression constructorCalleeExpression, P data) {
        super.visitConstructorCalleeExpression(constructorCalleeExpression, data);
    }

    public void visitTypeParameterListVoid(@NotNull JetTypeParameterList list, P data) {
        super.visitTypeParameterList(list, data);
    }

    public void visitTypeParameterVoid(@NotNull JetTypeParameter parameter, P data) {
        super.visitTypeParameter(parameter, data);
    }

    public void visitEnumEntryVoid(@NotNull JetEnumEntry enumEntry, P data) {
        super.visitEnumEntry(enumEntry, data);
    }

    public void visitParameterListVoid(@NotNull JetParameterList list, P data) {
        super.visitParameterList(list, data);
    }

    public void visitParameterVoid(@NotNull JetParameter parameter, P data) {
        super.visitParameter(parameter, data);
    }

    public void visitDelegationSpecifierListVoid(@NotNull JetDelegationSpecifierList list, P data) {
        super.visitDelegationSpecifierList(list, data);
    }

    public void visitDelegationSpecifierVoid(@NotNull JetDelegationSpecifier specifier, P data) {
        super.visitDelegationSpecifier(specifier, data);
    }

    public void visitDelegationByExpressionSpecifierVoid(@NotNull JetDelegatorByExpressionSpecifier specifier, P data) {
        super.visitDelegationByExpressionSpecifier(specifier, data);
    }

    public void visitDelegationToSuperCallSpecifierVoid(@NotNull JetDelegatorToSuperCall call, P data) {
        super.visitDelegationToSuperCallSpecifier(call, data);
    }

    public void visitDelegationToSuperClassSpecifierVoid(@NotNull JetDelegatorToSuperClass specifier, P data) {
        super.visitDelegationToSuperClassSpecifier(specifier, data);
    }

    public void visitDelegationCallVoid(@NotNull JetConstructorDelegationCall call, P data) {
        super.visitConstructorDelegationCall(call, data);
    }

    public void visitPropertyDelegateVoid(@NotNull JetPropertyDelegate delegate, P data) {
        super.visitPropertyDelegate(delegate, data);
    }

    public void visitTypeReferenceVoid(@NotNull JetTypeReference typeReference, P data) {
        super.visitTypeReference(typeReference, data);
    }

    public void visitValueArgumentListVoid(@NotNull JetValueArgumentList list, P data) {
        super.visitValueArgumentList(list, data);
    }

    public void visitArgumentVoid(@NotNull JetValueArgument argument, P data) {
        super.visitArgument(argument, data);
    }

    public void visitExpressionVoid(@NotNull JetExpression expression, P data) {
        super.visitExpression(expression, data);
    }

    public void visitLoopExpressionVoid(@NotNull JetLoopExpression loopExpression, P data) {
        super.visitLoopExpression(loopExpression, data);
    }

    public void visitConstantExpressionVoid(@NotNull JetConstantExpression expression, P data) {
        super.visitConstantExpression(expression, data);
    }

    public void visitSimpleNameExpressionVoid(@NotNull JetSimpleNameExpression expression, P data) {
        super.visitSimpleNameExpression(expression, data);
    }

    public void visitReferenceExpressionVoid(@NotNull JetReferenceExpression expression, P data) {
        super.visitReferenceExpression(expression, data);
    }

    public void visitLabeledExpressionVoid(@NotNull JetLabeledExpression expression, P data) {
        super.visitLabeledExpression(expression, data);
    }

    public void visitPrefixExpressionVoid(@NotNull JetPrefixExpression expression, P data) {
        super.visitPrefixExpression(expression, data);
    }

    public void visitPostfixExpressionVoid(@NotNull JetPostfixExpression expression, P data) {
        super.visitPostfixExpression(expression, data);
    }

    public void visitUnaryExpressionVoid(@NotNull JetUnaryExpression expression, P data) {
        super.visitUnaryExpression(expression, data);
    }

    public void visitBinaryExpressionVoid(@NotNull JetBinaryExpression expression, P data) {
        super.visitBinaryExpression(expression, data);
    }

    public void visitReturnExpressionVoid(@NotNull JetReturnExpression expression, P data) {
        super.visitReturnExpression(expression, data);
    }

    public void visitExpressionWithLabelVoid(@NotNull JetExpressionWithLabel expression, P data) {
        super.visitExpressionWithLabel(expression, data);
    }

    public void visitThrowExpressionVoid(@NotNull JetThrowExpression expression, P data) {
        super.visitThrowExpression(expression, data);
    }

    public void visitBreakExpressionVoid(@NotNull JetBreakExpression expression, P data) {
        super.visitBreakExpression(expression, data);
    }

    public void visitContinueExpressionVoid(@NotNull JetContinueExpression expression, P data) {
        super.visitContinueExpression(expression, data);
    }

    public void visitIfExpressionVoid(@NotNull JetIfExpression expression, P data) {
        super.visitIfExpression(expression, data);
    }

    public void visitWhenExpressionVoid(@NotNull JetWhenExpression expression, P data) {
        super.visitWhenExpression(expression, data);
    }

    public void visitTryExpressionVoid(@NotNull JetTryExpression expression, P data) {
        super.visitTryExpression(expression, data);
    }

    public void visitForExpressionVoid(@NotNull JetForExpression expression, P data) {
        super.visitForExpression(expression, data);
    }

    public void visitWhileExpressionVoid(@NotNull JetWhileExpression expression, P data) {
        super.visitWhileExpression(expression, data);
    }

    public void visitDoWhileExpressionVoid(@NotNull JetDoWhileExpression expression, P data) {
        super.visitDoWhileExpression(expression, data);
    }

    public void visitFunctionLiteralExpressionVoid(@NotNull JetFunctionLiteralExpression expression, P data) {
        super.visitFunctionLiteralExpression(expression, data);
    }

    public void visitAnnotatedExpressionVoid(@NotNull JetAnnotatedExpression expression, P data) {
        super.visitAnnotatedExpression(expression, data);
    }

    public void visitCallExpressionVoid(@NotNull JetCallExpression expression, P data) {
        super.visitCallExpression(expression, data);
    }

    public void visitArrayAccessExpressionVoid(@NotNull JetArrayAccessExpression expression, P data) {
        super.visitArrayAccessExpression(expression, data);
    }

    public void visitQualifiedExpressionVoid(@NotNull JetQualifiedExpression expression, P data) {
        super.visitQualifiedExpression(expression, data);
    }

    public void visitDoubleColonExpressionVoid(@NotNull JetDoubleColonExpression expression, P data) {
        super.visitDoubleColonExpression(expression, data);
    }

    public void visitCallableReferenceExpressionVoid(@NotNull JetCallableReferenceExpression expression, P data) {
        super.visitCallableReferenceExpression(expression, data);
    }

    public void visitClassLiteralExpressionVoid(@NotNull JetClassLiteralExpression expression, P data) {
        super.visitClassLiteralExpression(expression, data);
    }

    public void visitDotQualifiedExpressionVoid(@NotNull JetDotQualifiedExpression expression, P data) {
        super.visitDotQualifiedExpression(expression, data);
    }

    public void visitSafeQualifiedExpressionVoid(@NotNull JetSafeQualifiedExpression expression, P data) {
        super.visitSafeQualifiedExpression(expression, data);
    }

    public void visitObjectLiteralExpressionVoid(@NotNull JetObjectLiteralExpression expression, P data) {
        super.visitObjectLiteralExpression(expression, data);
    }

    public void visitRootPackageExpressionVoid(@NotNull JetRootPackageExpression expression, P data) {
        super.visitRootPackageExpression(expression, data);
    }

    public void visitBlockExpressionVoid(@NotNull JetBlockExpression expression, P data) {
        super.visitBlockExpression(expression, data);
    }

    public void visitCatchSectionVoid(@NotNull JetCatchClause catchClause, P data) {
        super.visitCatchSection(catchClause, data);
    }

    public void visitFinallySectionVoid(@NotNull JetFinallySection finallySection, P data) {
        super.visitFinallySection(finallySection, data);
    }

    public void visitTypeArgumentListVoid(@NotNull JetTypeArgumentList typeArgumentList, P data) {
        super.visitTypeArgumentList(typeArgumentList, data);
    }

    public void visitThisExpressionVoid(@NotNull JetThisExpression expression, P data) {
        super.visitThisExpression(expression, data);
    }

    public void visitSuperExpressionVoid(@NotNull JetSuperExpression expression, P data) {
        super.visitSuperExpression(expression, data);
    }

    public void visitParenthesizedExpressionVoid(@NotNull JetParenthesizedExpression expression, P data) {
        super.visitParenthesizedExpression(expression, data);
    }

    public void visitInitializerListVoid(@NotNull JetInitializerList list, P data) {
        super.visitInitializerList(list, data);
    }

    public void visitAnonymousInitializerVoid(@NotNull JetClassInitializer initializer, P data) {
        super.visitAnonymousInitializer(initializer, data);
    }

    public void visitPropertyAccessorVoid(@NotNull JetPropertyAccessor accessor, P data) {
        super.visitPropertyAccessor(accessor, data);
    }

    public void visitTypeConstraintListVoid(@NotNull JetTypeConstraintList list, P data) {
        super.visitTypeConstraintList(list, data);
    }

    public void visitTypeConstraintVoid(@NotNull JetTypeConstraint constraint, P data) {
        super.visitTypeConstraint(constraint, data);
    }

    public void visitUserTypeVoid(@NotNull JetUserType type, P data) {
        super.visitUserType(type, data);
    }

    public void visitDynamicTypeVoid(@NotNull JetDynamicType type, P data) {
        super.visitDynamicType(type, data);
    }

    public void visitFunctionTypeVoid(@NotNull JetFunctionType type, P data) {
        super.visitFunctionType(type, data);
    }

    public void visitSelfTypeVoid(@NotNull JetSelfType type, P data) {
        super.visitSelfType(type, data);
    }

    public void visitBinaryWithTypeRHSExpressionVoid(@NotNull JetBinaryExpressionWithTypeRHS expression, P data) {
        super.visitBinaryWithTypeRHSExpression(expression, data);
    }

    public void visitStringTemplateExpressionVoid(@NotNull JetStringTemplateExpression expression, P data) {
        super.visitStringTemplateExpression(expression, data);
    }

    public void visitNamedDeclarationVoid(@NotNull JetNamedDeclaration declaration, P data) {
        super.visitNamedDeclaration(declaration, data);
    }

    public void visitNullableTypeVoid(@NotNull JetNullableType nullableType, P data) {
        super.visitNullableType(nullableType, data);
    }

    public void visitTypeProjectionVoid(@NotNull JetTypeProjection typeProjection, P data) {
        super.visitTypeProjection(typeProjection, data);
    }

    public void visitWhenEntryVoid(@NotNull JetWhenEntry jetWhenEntry, P data) {
        super.visitWhenEntry(jetWhenEntry, data);
    }

    public void visitIsExpressionVoid(@NotNull JetIsExpression expression, P data) {
        super.visitIsExpression(expression, data);
    }

    public void visitWhenConditionIsPatternVoid(@NotNull JetWhenConditionIsPattern condition, P data) {
        super.visitWhenConditionIsPattern(condition, data);
    }

    public void visitWhenConditionInRangeVoid(@NotNull JetWhenConditionInRange condition, P data) {
        super.visitWhenConditionInRange(condition, data);
    }

    public void visitWhenConditionWithExpressionVoid(@NotNull JetWhenConditionWithExpression condition, P data) {
        super.visitWhenConditionWithExpression(condition, data);
    }

    public void visitObjectDeclarationVoid(@NotNull JetObjectDeclaration declaration, P data) {
        super.visitObjectDeclaration(declaration, data);
    }

    public void visitObjectDeclarationNameVoid(@NotNull JetObjectDeclarationName declarationName, P data) {
        super.visitObjectDeclarationName(declarationName, data);
    }

    public void visitStringTemplateEntryVoid(@NotNull JetStringTemplateEntry entry, P data) {
        super.visitStringTemplateEntry(entry, data);
    }

    public void visitStringTemplateEntryWithExpressionVoid(@NotNull JetStringTemplateEntryWithExpression entry, P data) {
        super.visitStringTemplateEntryWithExpression(entry, data);
    }

    public void visitBlockStringTemplateEntryVoid(@NotNull JetBlockStringTemplateEntry entry, P data) {
        super.visitBlockStringTemplateEntry(entry, data);
    }

    public void visitSimpleNameStringTemplateEntryVoid(@NotNull JetSimpleNameStringTemplateEntry entry, P data) {
        super.visitSimpleNameStringTemplateEntry(entry, data);
    }

    public void visitLiteralStringTemplateEntryVoid(@NotNull JetLiteralStringTemplateEntry entry, P data) {
        super.visitLiteralStringTemplateEntry(entry, data);
    }

    public void visitEscapeStringTemplateEntryVoid(@NotNull JetEscapeStringTemplateEntry entry, P data) {
        super.visitEscapeStringTemplateEntry(entry, data);
    }

    // hidden methods
    @Override
    public final Void visitJetElement(@NotNull JetElement element, P data) {
        visitJetElementVoid(element, data);
    	return null;
    }

    @Override
    public final Void visitDeclaration(@NotNull JetDeclaration dcl, P data) {
        visitDeclarationVoid(dcl, data);
    	return null;
    }

    @Override
    public final Void visitClass(@NotNull JetClass klass, P data) {
        visitClassVoid(klass, data);
    	return null;
    }

    @Override
    public final Void visitNamedFunction(@NotNull JetNamedFunction function, P data) {
        visitNamedFunctionVoid(function, data);
    	return null;
    }

    @Override
    public final Void visitProperty(@NotNull JetProperty property, P data) {
        visitPropertyVoid(property, data);
    	return null;
    }

    @Override
    public final Void visitMultiDeclaration(@NotNull JetMultiDeclaration multiDeclaration, P data) {
        visitMultiDeclarationVoid(multiDeclaration, data);
    	return null;
    }

    @Override
    public final Void visitMultiDeclarationEntry(@NotNull JetMultiDeclarationEntry multiDeclarationEntry, P data) {
        visitMultiDeclarationEntryVoid(multiDeclarationEntry, data);
    	return null;
    }

    @Override
    public final Void visitTypedef(@NotNull JetTypedef typedef, P data) {
        visitTypedefVoid(typedef, data);
        return null;
    }

    @Override
    public final Void visitJetFile(@NotNull JetFile file, P data) {
        visitJetFileVoid(file, data);
        return null;
    }

    @Override
    public final Void visitScript(@NotNull JetScript script, P data) {
        visitScriptVoid(script, data);
        return null;
    }

    @Override
    public final Void visitImportDirective(@NotNull JetImportDirective importDirective, P data) {
        visitImportDirectiveVoid(importDirective, data);
    	return null;
    }

    @Override
    public final Void visitImportList(@NotNull JetImportList importList, P data) {
        visitImportListVoid(importList, data);
    	return null;
    }

    @Override
    public final Void visitClassBody(@NotNull JetClassBody classBody, P data) {
        visitClassBodyVoid(classBody, data);
    	return null;
    }

    @Override
    public final Void visitModifierList(@NotNull JetModifierList list, P data) {
        visitModifierListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitAnnotation(@NotNull JetAnnotation annotation, P data) {
        visitAnnotationVoid(annotation, data);
    	return null;
    }

    @Override
    public final Void visitAnnotationEntry(@NotNull JetAnnotationEntry annotationEntry, P data) {
        visitAnnotationEntryVoid(annotationEntry, data);
    	return null;
    }

    @Override
    public final Void visitTypeParameterList(@NotNull JetTypeParameterList list, P data) {
        visitTypeParameterListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitTypeParameter(@NotNull JetTypeParameter parameter, P data) {
        visitTypeParameterVoid(parameter, data);
    	return null;
    }

    @Override
    public final Void visitEnumEntry(@NotNull JetEnumEntry enumEntry, P data) {
        visitEnumEntryVoid(enumEntry, data);
    	return null;
    }

    @Override
    public final Void visitParameterList(@NotNull JetParameterList list, P data) {
        visitParameterListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitParameter(@NotNull JetParameter parameter, P data) {
        visitParameterVoid(parameter, data);
    	return null;
    }

    @Override
    public final Void visitDelegationSpecifierList(@NotNull JetDelegationSpecifierList list, P data) {
        visitDelegationSpecifierListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitDelegationSpecifier(@NotNull JetDelegationSpecifier specifier, P data) {
        visitDelegationSpecifierVoid(specifier, data);
    	return null;
    }

    @Override
    public final Void visitDelegationByExpressionSpecifier(
            @NotNull JetDelegatorByExpressionSpecifier specifier, P data
    ) {
        visitDelegationByExpressionSpecifierVoid(specifier, data);
    	return null;
    }

    @Override
    public final Void visitDelegationToSuperCallSpecifier(@NotNull JetDelegatorToSuperCall call, P data) {
        visitDelegationToSuperCallSpecifierVoid(call, data);
    	return null;
    }

    @Override
    public final Void visitDelegationToSuperClassSpecifier(@NotNull JetDelegatorToSuperClass specifier, P data) {
        visitDelegationToSuperClassSpecifierVoid(specifier, data);
    	return null;
    }

    @Override
    public final Void visitConstructorDelegationCall(@NotNull JetConstructorDelegationCall call, P data) {
        visitDelegationCallVoid(call, data);
        return null;
    }

    @Override
    public final Void visitPropertyDelegate(@NotNull JetPropertyDelegate delegate, P data) {
        visitPropertyDelegateVoid(delegate, data);
    	return null;
    }

    @Override
    public final Void visitTypeReference(@NotNull JetTypeReference typeReference, P data) {
        visitTypeReferenceVoid(typeReference, data);
    	return null;
    }

    @Override
    public final Void visitValueArgumentList(@NotNull JetValueArgumentList list, P data) {
        visitValueArgumentListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitArgument(@NotNull JetValueArgument argument, P data) {
        visitArgumentVoid(argument, data);
    	return null;
    }

    @Override
    public final Void visitExpression(@NotNull JetExpression expression, P data) {
        visitExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitLoopExpression(@NotNull JetLoopExpression loopExpression, P data) {
        visitLoopExpressionVoid(loopExpression, data);
    	return null;
    }

    @Override
    public final Void visitConstantExpression(@NotNull JetConstantExpression expression, P data) {
        visitConstantExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression, P data) {
        visitSimpleNameExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitReferenceExpression(@NotNull JetReferenceExpression expression, P data) {
        visitReferenceExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitLabeledExpression(@NotNull JetLabeledExpression expression, P data) {
        visitLabeledExpressionVoid(expression, data);
        return null;
    }

    @Override
    public final Void visitPrefixExpression(@NotNull JetPrefixExpression expression, P data) {
        visitPrefixExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitPostfixExpression(@NotNull JetPostfixExpression expression, P data) {
        visitPostfixExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitUnaryExpression(@NotNull JetUnaryExpression expression, P data) {
        visitUnaryExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitBinaryExpression(@NotNull JetBinaryExpression expression, P data) {
        visitBinaryExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitReturnExpression(@NotNull JetReturnExpression expression, P data) {
        visitReturnExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitExpressionWithLabel(@NotNull JetExpressionWithLabel expression, P data) {
        visitExpressionWithLabelVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitThrowExpression(@NotNull JetThrowExpression expression, P data) {
        visitThrowExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitBreakExpression(@NotNull JetBreakExpression expression, P data) {
        visitBreakExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitContinueExpression(@NotNull JetContinueExpression expression, P data) {
        visitContinueExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitIfExpression(@NotNull JetIfExpression expression, P data) {
        visitIfExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitWhenExpression(@NotNull JetWhenExpression expression, P data) {
        visitWhenExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitTryExpression(@NotNull JetTryExpression expression, P data) {
        visitTryExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitForExpression(@NotNull JetForExpression expression, P data) {
        visitForExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitWhileExpression(@NotNull JetWhileExpression expression, P data) {
        visitWhileExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitDoWhileExpression(@NotNull JetDoWhileExpression expression, P data) {
        visitDoWhileExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression, P data) {
        visitFunctionLiteralExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitAnnotatedExpression(@NotNull JetAnnotatedExpression expression, P data) {
        visitAnnotatedExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitCallExpression(@NotNull JetCallExpression expression, P data) {
        visitCallExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression, P data) {
        visitArrayAccessExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitQualifiedExpression(@NotNull JetQualifiedExpression expression, P data) {
        visitQualifiedExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitDoubleColonExpression(@NotNull JetDoubleColonExpression expression, P data) {
        visitDoubleColonExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitCallableReferenceExpression(@NotNull JetCallableReferenceExpression expression, P data) {
        visitCallableReferenceExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitClassLiteralExpression(@NotNull JetClassLiteralExpression expression, P data) {
        visitClassLiteralExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitDotQualifiedExpression(@NotNull JetDotQualifiedExpression expression, P data) {
        visitDotQualifiedExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitSafeQualifiedExpression(@NotNull JetSafeQualifiedExpression expression, P data) {
        visitSafeQualifiedExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitObjectLiteralExpression(@NotNull JetObjectLiteralExpression expression, P data) {
        visitObjectLiteralExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitRootPackageExpression(@NotNull JetRootPackageExpression expression, P data) {
        visitRootPackageExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitBlockExpression(@NotNull JetBlockExpression expression, P data) {
        visitBlockExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitCatchSection(@NotNull JetCatchClause catchClause, P data) {
        visitCatchSectionVoid(catchClause, data);
    	return null;
    }

    @Override
    public final Void visitFinallySection(@NotNull JetFinallySection finallySection, P data) {
        visitFinallySectionVoid(finallySection, data);
    	return null;
    }

    @Override
    public final Void visitTypeArgumentList(@NotNull JetTypeArgumentList typeArgumentList, P data) {
        visitTypeArgumentListVoid(typeArgumentList, data);
    	return null;
    }

    @Override
    public final Void visitThisExpression(@NotNull JetThisExpression expression, P data) {
        visitThisExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitSuperExpression(@NotNull JetSuperExpression expression, P data) {
        visitSuperExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression, P data) {
        visitParenthesizedExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitInitializerList(@NotNull JetInitializerList list, P data) {
        visitInitializerListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitAnonymousInitializer(@NotNull JetClassInitializer initializer, P data) {
        visitAnonymousInitializerVoid(initializer, data);
    	return null;
    }

    @Override
    public final Void visitPropertyAccessor(@NotNull JetPropertyAccessor accessor, P data) {
        visitPropertyAccessorVoid(accessor, data);
    	return null;
    }

    @Override
    public final Void visitTypeConstraintList(@NotNull JetTypeConstraintList list, P data) {
        visitTypeConstraintListVoid(list, data);
    	return null;
    }

    @Override
    public final Void visitTypeConstraint(@NotNull JetTypeConstraint constraint, P data) {
        visitTypeConstraintVoid(constraint, data);
    	return null;
    }

    @Override
    public final Void visitUserType(@NotNull JetUserType type, P data) {
        visitUserTypeVoid(type, data);
    	return null;
    }

    @Override
    public Void visitDynamicType(@NotNull JetDynamicType type, P data) {
        visitDynamicTypeVoid(type, data);
        return null;
    }

    @Override
    public final Void visitFunctionType(@NotNull JetFunctionType type, P data) {
        visitFunctionTypeVoid(type, data);
    	return null;
    }

    @Override
    public final Void visitSelfType(@NotNull JetSelfType type, P data) {
        visitSelfTypeVoid(type, data);
    	return null;
    }

    @Override
    public final Void visitBinaryWithTypeRHSExpression(@NotNull JetBinaryExpressionWithTypeRHS expression, P data) {
        visitBinaryWithTypeRHSExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression, P data) {
        visitStringTemplateExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitNamedDeclaration(@NotNull JetNamedDeclaration declaration, P data) {
        visitNamedDeclarationVoid(declaration, data);
    	return null;
    }

    @Override
    public final Void visitNullableType(@NotNull JetNullableType nullableType, P data) {
        visitNullableTypeVoid(nullableType, data);
    	return null;
    }

    @Override
    public final Void visitTypeProjection(@NotNull JetTypeProjection typeProjection, P data) {
        visitTypeProjectionVoid(typeProjection, data);
    	return null;
    }

    @Override
    public final Void visitWhenEntry(@NotNull JetWhenEntry jetWhenEntry, P data) {
        visitWhenEntryVoid(jetWhenEntry, data);
    	return null;
    }

    @Override
    public final Void visitIsExpression(@NotNull JetIsExpression expression, P data) {
        visitIsExpressionVoid(expression, data);
    	return null;
    }

    @Override
    public final Void visitWhenConditionIsPattern(@NotNull JetWhenConditionIsPattern condition, P data) {
        visitWhenConditionIsPatternVoid(condition, data);
    	return null;
    }

    @Override
    public final Void visitWhenConditionInRange(@NotNull JetWhenConditionInRange condition, P data) {
        visitWhenConditionInRangeVoid(condition, data);
    	return null;
    }

    @Override
    public final Void visitWhenConditionWithExpression(@NotNull JetWhenConditionWithExpression condition, P data) {
        visitWhenConditionWithExpressionVoid(condition, data);
    	return null;
    }

    @Override
    public final Void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, P data) {
        visitObjectDeclarationVoid(declaration, data);
    	return null;
    }

    @Override
    public final Void visitObjectDeclarationName(@NotNull JetObjectDeclarationName declarationName, P data) {
        visitObjectDeclarationNameVoid(declarationName, data);
    	return null;
    }

    @Override
    public final Void visitStringTemplateEntry(@NotNull JetStringTemplateEntry entry, P data) {
        visitStringTemplateEntryVoid(entry, data);
    	return null;
    }

    @Override
    public final Void visitStringTemplateEntryWithExpression(@NotNull JetStringTemplateEntryWithExpression entry, P data) {
        visitStringTemplateEntryWithExpressionVoid(entry, data);
    	return null;
    }

    @Override
    public final Void visitBlockStringTemplateEntry(@NotNull JetBlockStringTemplateEntry entry, P data) {
        visitBlockStringTemplateEntryVoid(entry, data);
    	return null;
    }

    @Override
    public final Void visitSimpleNameStringTemplateEntry(@NotNull JetSimpleNameStringTemplateEntry entry, P data) {
        visitSimpleNameStringTemplateEntryVoid(entry, data);
    	return null;
    }

    @Override
    public final Void visitLiteralStringTemplateEntry(@NotNull JetLiteralStringTemplateEntry entry, P data) {
        visitLiteralStringTemplateEntryVoid(entry, data);
    	return null;
    }

    @Override
    public final Void visitEscapeStringTemplateEntry(@NotNull JetEscapeStringTemplateEntry entry, P data) {
        visitEscapeStringTemplateEntryVoid(entry, data);
        return null;
    }
}
