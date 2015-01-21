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

public class JetVisitorVoid extends JetVisitor<Void, Void> {
    // methods with void return

    public void visitJetElement(@NotNull JetElement element) {
        super.visitJetElement(element, null);
    }

    public void visitDeclaration(@NotNull JetDeclaration dcl) {
        super.visitDeclaration(dcl, null);
    }

    public void visitClass(@NotNull JetClass klass) {
        super.visitClass(klass, null);
    }

    public void visitSecondaryConstructor(@NotNull JetSecondaryConstructor constructor) {
        super.visitSecondaryConstructor(constructor, null);
    }

    public void visitNamedFunction(@NotNull JetNamedFunction function) {
        super.visitNamedFunction(function, null);
    }

    public void visitProperty(@NotNull JetProperty property) {
        super.visitProperty(property, null);
    }

    public void visitMultiDeclaration(@NotNull JetMultiDeclaration multiDeclaration) {
        super.visitMultiDeclaration(multiDeclaration, null);
    }

    public void visitMultiDeclarationEntry(@NotNull JetMultiDeclarationEntry multiDeclarationEntry) {
        super.visitMultiDeclarationEntry(multiDeclarationEntry, null);
    }

    public void visitTypedef(@NotNull JetTypedef typedef) {
        super.visitTypedef(typedef, null);
    }

    public void visitJetFile(@NotNull JetFile file) {
        super.visitJetFile(file, null);
    }

    public void visitScript(@NotNull JetScript script) {
        super.visitScript(script, null);
    }

    public void visitImportDirective(@NotNull JetImportDirective importDirective) {
        super.visitImportDirective(importDirective, null);
    }

    public void visitImportList(@NotNull JetImportList importList) {
        super.visitImportList(importList, null);
    }

    public void visitClassBody(@NotNull JetClassBody classBody) {
        super.visitClassBody(classBody, null);
    }

    public void visitModifierList(@NotNull JetModifierList list) {
        super.visitModifierList(list, null);
    }

    public void visitAnnotation(@NotNull JetAnnotation annotation) {
        super.visitAnnotation(annotation, null);
    }

    public void visitAnnotationEntry(@NotNull JetAnnotationEntry annotationEntry) {
        super.visitAnnotationEntry(annotationEntry, null);
    }

    public void visitConstructorCalleeExpression(@NotNull JetConstructorCalleeExpression constructorCalleeExpression) {
        super.visitConstructorCalleeExpression(constructorCalleeExpression, null);
    }

    public void visitTypeParameterList(@NotNull JetTypeParameterList list) {
        super.visitTypeParameterList(list, null);
    }

    public void visitTypeParameter(@NotNull JetTypeParameter parameter) {
        super.visitTypeParameter(parameter, null);
    }

    public void visitEnumEntry(@NotNull JetEnumEntry enumEntry) {
        super.visitEnumEntry(enumEntry, null);
    }

    public void visitParameterList(@NotNull JetParameterList list) {
        super.visitParameterList(list, null);
    }

    public void visitParameter(@NotNull JetParameter parameter) {
        super.visitParameter(parameter, null);
    }

    public void visitDelegationSpecifierList(@NotNull JetDelegationSpecifierList list) {
        super.visitDelegationSpecifierList(list, null);
    }

    public void visitDelegationSpecifier(@NotNull JetDelegationSpecifier specifier) {
        super.visitDelegationSpecifier(specifier, null);
    }

    public void visitDelegationByExpressionSpecifier(@NotNull JetDelegatorByExpressionSpecifier specifier) {
        super.visitDelegationByExpressionSpecifier(specifier, null);
    }

    public void visitDelegationToSuperCallSpecifier(@NotNull JetDelegatorToSuperCall call) {
        super.visitDelegationToSuperCallSpecifier(call, null);
    }

    public void visitDelegationToSuperClassSpecifier(@NotNull JetDelegatorToSuperClass specifier) {
        super.visitDelegationToSuperClassSpecifier(specifier, null);
    }

    public void visitConstructorDelegationCall(@NotNull JetConstructorDelegationCall call) {
        super.visitConstructorDelegationCall(call, null);
    }

    public void visitPropertyDelegate(@NotNull JetPropertyDelegate delegate) {
        super.visitPropertyDelegate(delegate, null);
    }

    public void visitTypeReference(@NotNull JetTypeReference typeReference) {
        super.visitTypeReference(typeReference, null);
    }

    public void visitValueArgumentList(@NotNull JetValueArgumentList list) {
        super.visitValueArgumentList(list, null);
    }

    public void visitArgument(@NotNull JetValueArgument argument) {
        super.visitArgument(argument, null);
    }

    public void visitExpression(@NotNull JetExpression expression) {
        super.visitExpression(expression, null);
    }

    public void visitLoopExpression(@NotNull JetLoopExpression loopExpression) {
        super.visitLoopExpression(loopExpression, null);
    }

    public void visitConstantExpression(@NotNull JetConstantExpression expression) {
        super.visitConstantExpression(expression, null);
    }

    public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression) {
        super.visitSimpleNameExpression(expression, null);
    }

    public void visitReferenceExpression(@NotNull JetReferenceExpression expression) {
        super.visitReferenceExpression(expression, null);
    }

    public void visitLabeledExpression(@NotNull JetLabeledExpression expression) {
        super.visitLabeledExpression(expression, null);
    }

    public void visitPrefixExpression(@NotNull JetPrefixExpression expression) {
        super.visitPrefixExpression(expression, null);
    }

    public void visitPostfixExpression(@NotNull JetPostfixExpression expression) {
        super.visitPostfixExpression(expression, null);
    }

    public void visitUnaryExpression(@NotNull JetUnaryExpression expression) {
        super.visitUnaryExpression(expression, null);
    }

    public void visitBinaryExpression(@NotNull JetBinaryExpression expression) {
        super.visitBinaryExpression(expression, null);
    }

    public void visitReturnExpression(@NotNull JetReturnExpression expression) {
        super.visitReturnExpression(expression, null);
    }

    public void visitExpressionWithLabel(@NotNull JetExpressionWithLabel expression) {
        super.visitExpressionWithLabel(expression, null);
    }

    public void visitThrowExpression(@NotNull JetThrowExpression expression) {
        super.visitThrowExpression(expression, null);
    }

    public void visitBreakExpression(@NotNull JetBreakExpression expression) {
        super.visitBreakExpression(expression, null);
    }

    public void visitContinueExpression(@NotNull JetContinueExpression expression) {
        super.visitContinueExpression(expression, null);
    }

    public void visitIfExpression(@NotNull JetIfExpression expression) {
        super.visitIfExpression(expression, null);
    }

    public void visitWhenExpression(@NotNull JetWhenExpression expression) {
        super.visitWhenExpression(expression, null);
    }

    public void visitTryExpression(@NotNull JetTryExpression expression) {
        super.visitTryExpression(expression, null);
    }

    public void visitForExpression(@NotNull JetForExpression expression) {
        super.visitForExpression(expression, null);
    }

    public void visitWhileExpression(@NotNull JetWhileExpression expression) {
        super.visitWhileExpression(expression, null);
    }

    public void visitDoWhileExpression(@NotNull JetDoWhileExpression expression) {
        super.visitDoWhileExpression(expression, null);
    }

    public void visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression) {
        super.visitFunctionLiteralExpression(expression, null);
    }

    public void visitAnnotatedExpression(@NotNull JetAnnotatedExpression expression) {
        super.visitAnnotatedExpression(expression, null);
    }

    public void visitCallExpression(@NotNull JetCallExpression expression) {
        super.visitCallExpression(expression, null);
    }

    public void visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression) {
        super.visitArrayAccessExpression(expression, null);
    }

    public void visitQualifiedExpression(@NotNull JetQualifiedExpression expression) {
        super.visitQualifiedExpression(expression, null);
    }

    public void visitDoubleColonExpression(@NotNull JetDoubleColonExpression expression) {
        super.visitDoubleColonExpression(expression, null);
    }

    public void visitCallableReferenceExpression(@NotNull JetCallableReferenceExpression expression) {
        super.visitCallableReferenceExpression(expression, null);
    }

    public void visitClassLiteralExpression(@NotNull JetClassLiteralExpression expression) {
        super.visitClassLiteralExpression(expression, null);
    }

    public void visitDotQualifiedExpression(@NotNull JetDotQualifiedExpression expression) {
        super.visitDotQualifiedExpression(expression, null);
    }

    public void visitSafeQualifiedExpression(@NotNull JetSafeQualifiedExpression expression) {
        super.visitSafeQualifiedExpression(expression, null);
    }

    public void visitObjectLiteralExpression(@NotNull JetObjectLiteralExpression expression) {
        super.visitObjectLiteralExpression(expression, null);
    }

    public void visitRootPackageExpression(@NotNull JetRootPackageExpression expression) {
        super.visitRootPackageExpression(expression, null);
    }

    public void visitBlockExpression(@NotNull JetBlockExpression expression) {
        super.visitBlockExpression(expression, null);
    }

    public void visitCatchSection(@NotNull JetCatchClause catchClause) {
        super.visitCatchSection(catchClause, null);
    }

    public void visitFinallySection(@NotNull JetFinallySection finallySection) {
        super.visitFinallySection(finallySection, null);
    }

    public void visitTypeArgumentList(@NotNull JetTypeArgumentList typeArgumentList) {
        super.visitTypeArgumentList(typeArgumentList, null);
    }

    public void visitThisExpression(@NotNull JetThisExpression expression) {
        super.visitThisExpression(expression, null);
    }

    public void visitSuperExpression(@NotNull JetSuperExpression expression) {
        super.visitSuperExpression(expression, null);
    }

    public void visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression) {
        super.visitParenthesizedExpression(expression, null);
    }

    public void visitInitializerList(@NotNull JetInitializerList list) {
        super.visitInitializerList(list, null);
    }

    public void visitAnonymousInitializer(@NotNull JetClassInitializer initializer) {
        super.visitAnonymousInitializer(initializer, null);
    }

    public void visitPropertyAccessor(@NotNull JetPropertyAccessor accessor) {
        super.visitPropertyAccessor(accessor, null);
    }

    public void visitTypeConstraintList(@NotNull JetTypeConstraintList list) {
        super.visitTypeConstraintList(list, null);
    }

    public void visitTypeConstraint(@NotNull JetTypeConstraint constraint) {
        super.visitTypeConstraint(constraint, null);
    }

    public void visitUserType(@NotNull JetUserType type) {
        super.visitUserType(type, null);
    }

    public void visitDynamicType(@NotNull JetDynamicType type) {
        super.visitDynamicType(type, null);
    }

    public void visitFunctionType(@NotNull JetFunctionType type) {
        super.visitFunctionType(type, null);
    }

    public void visitSelfType(@NotNull JetSelfType type) {
        super.visitSelfType(type, null);
    }

    public void visitBinaryWithTypeRHSExpression(@NotNull JetBinaryExpressionWithTypeRHS expression) {
        super.visitBinaryWithTypeRHSExpression(expression, null);
    }

    public void visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression) {
        super.visitStringTemplateExpression(expression, null);
    }

    public void visitNamedDeclaration(@NotNull JetNamedDeclaration declaration) {
        super.visitNamedDeclaration(declaration, null);
    }

    public void visitNullableType(@NotNull JetNullableType nullableType) {
        super.visitNullableType(nullableType, null);
    }

    public void visitTypeProjection(@NotNull JetTypeProjection typeProjection) {
        super.visitTypeProjection(typeProjection, null);
    }

    public void visitWhenEntry(@NotNull JetWhenEntry jetWhenEntry) {
        super.visitWhenEntry(jetWhenEntry, null);
    }

    public void visitIsExpression(@NotNull JetIsExpression expression) {
        super.visitIsExpression(expression, null);
    }

    public void visitWhenConditionIsPattern(@NotNull JetWhenConditionIsPattern condition) {
        super.visitWhenConditionIsPattern(condition, null);
    }

    public void visitWhenConditionInRange(@NotNull JetWhenConditionInRange condition) {
        super.visitWhenConditionInRange(condition, null);
    }

    public void visitWhenConditionWithExpression(@NotNull JetWhenConditionWithExpression condition) {
        super.visitWhenConditionWithExpression(condition, null);
    }

    public void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration) {
        super.visitObjectDeclaration(declaration, null);
    }

    public void visitObjectDeclarationName(@NotNull JetObjectDeclarationName declarationName) {
        super.visitObjectDeclarationName(declarationName, null);
    }

    public void visitStringTemplateEntry(@NotNull JetStringTemplateEntry entry) {
        super.visitStringTemplateEntry(entry, null);
    }

    public void visitStringTemplateEntryWithExpression(@NotNull JetStringTemplateEntryWithExpression entry) {
        super.visitStringTemplateEntryWithExpression(entry, null);
    }

    public void visitBlockStringTemplateEntry(@NotNull JetBlockStringTemplateEntry entry) {
        super.visitBlockStringTemplateEntry(entry, null);
    }

    public void visitSimpleNameStringTemplateEntry(@NotNull JetSimpleNameStringTemplateEntry entry) {
        super.visitSimpleNameStringTemplateEntry(entry, null);
    }

    public void visitLiteralStringTemplateEntry(@NotNull JetLiteralStringTemplateEntry entry) {
        super.visitLiteralStringTemplateEntry(entry, null);
    }

    public void visitEscapeStringTemplateEntry(@NotNull JetEscapeStringTemplateEntry entry) {
        super.visitEscapeStringTemplateEntry(entry, null);
    }

    public void visitPackageDirective(@NotNull JetPackageDirective directive) {
        super.visitPackageDirective(directive, null);
    }

    // hidden methods
    @Override
    public final Void visitJetElement(@NotNull JetElement element, Void data) {
        visitJetElement(element);
        return null;
    }

    @Override
    public final Void visitDeclaration(@NotNull JetDeclaration dcl, Void data) {
        visitDeclaration(dcl);
        return null;
    }

    @Override
    public final Void visitClass(@NotNull JetClass klass, Void data) {
        visitClass(klass);
        return null;
    }

    @Override
    public final Void visitSecondaryConstructor(@NotNull JetSecondaryConstructor constructor, Void data) {
        visitSecondaryConstructor(constructor);
        return null;
    }

    @Override
    public final Void visitNamedFunction(@NotNull JetNamedFunction function, Void data) {
        visitNamedFunction(function);
        return null;
    }

    @Override
    public final Void visitProperty(@NotNull JetProperty property, Void data) {
        visitProperty(property);
        return null;
    }

    @Override
    public final Void visitMultiDeclaration(@NotNull JetMultiDeclaration multiDeclaration, Void data) {
        visitMultiDeclaration(multiDeclaration);
        return null;
    }

    @Override
    public final Void visitMultiDeclarationEntry(@NotNull JetMultiDeclarationEntry multiDeclarationEntry, Void data) {
        visitMultiDeclarationEntry(multiDeclarationEntry);
        return null;
    }

    @Override
    public final Void visitTypedef(@NotNull JetTypedef typedef, Void data) {
        visitTypedef(typedef);
        return null;
    }

    @Override
    public final Void visitJetFile(@NotNull JetFile file, Void data) {
        visitJetFile(file);
        return null;
    }

    @Override
    public final Void visitScript(@NotNull JetScript script, Void data) {
        visitScript(script);
        return null;
    }

    @Override
    public final Void visitImportDirective(@NotNull JetImportDirective importDirective, Void data) {
        visitImportDirective(importDirective);
        return null;
    }

    @Override
    public final Void visitImportList(@NotNull JetImportList importList, Void data) {
        visitImportList(importList);
        return null;
    }

    @Override
    public final Void visitClassBody(@NotNull JetClassBody classBody, Void data) {
        visitClassBody(classBody);
        return null;
    }

    @Override
    public final Void visitModifierList(@NotNull JetModifierList list, Void data) {
        visitModifierList(list);
        return null;
    }

    @Override
    public final Void visitAnnotation(@NotNull JetAnnotation annotation, Void data) {
        visitAnnotation(annotation);
        return null;
    }

    @Override
    public final Void visitAnnotationEntry(@NotNull JetAnnotationEntry annotationEntry, Void data) {
        visitAnnotationEntry(annotationEntry);
        return null;
    }

    @Override
    public final Void visitTypeParameterList(@NotNull JetTypeParameterList list, Void data) {
        visitTypeParameterList(list);
        return null;
    }

    @Override
    public final Void visitTypeParameter(@NotNull JetTypeParameter parameter, Void data) {
        visitTypeParameter(parameter);
        return null;
    }

    @Override
    public final Void visitEnumEntry(@NotNull JetEnumEntry enumEntry, Void data) {
        visitEnumEntry(enumEntry);
        return null;
    }

    @Override
    public final Void visitParameterList(@NotNull JetParameterList list, Void data) {
        visitParameterList(list);
        return null;
    }

    @Override
    public final Void visitParameter(@NotNull JetParameter parameter, Void data) {
        visitParameter(parameter);
        return null;
    }

    @Override
    public final Void visitDelegationSpecifierList(@NotNull JetDelegationSpecifierList list, Void data) {
        visitDelegationSpecifierList(list);
        return null;
    }

    @Override
    public final Void visitDelegationSpecifier(@NotNull JetDelegationSpecifier specifier, Void data) {
        visitDelegationSpecifier(specifier);
        return null;
    }

    @Override
    public final Void visitDelegationByExpressionSpecifier(
            @NotNull JetDelegatorByExpressionSpecifier specifier, Void data
    ) {
        visitDelegationByExpressionSpecifier(specifier);
        return null;
    }

    @Override
    public final Void visitDelegationToSuperCallSpecifier(@NotNull JetDelegatorToSuperCall call, Void data) {
        visitDelegationToSuperCallSpecifier(call);
        return null;
    }

    @Override
    public final Void visitDelegationToSuperClassSpecifier(@NotNull JetDelegatorToSuperClass specifier, Void data) {
        visitDelegationToSuperClassSpecifier(specifier);
        return null;
    }

    @Override
    public final Void visitConstructorDelegationCall(@NotNull JetConstructorDelegationCall call, Void data) {
        visitConstructorDelegationCall(call);
        return null;
    }

    @Override
    public final Void visitPropertyDelegate(@NotNull JetPropertyDelegate delegate, Void data) {
        visitPropertyDelegate(delegate);
        return null;
    }

    @Override
    public final Void visitTypeReference(@NotNull JetTypeReference typeReference, Void data) {
        visitTypeReference(typeReference);
        return null;
    }

    @Override
    public final Void visitValueArgumentList(@NotNull JetValueArgumentList list, Void data) {
        visitValueArgumentList(list);
        return null;
    }

    @Override
    public final Void visitArgument(@NotNull JetValueArgument argument, Void data) {
        visitArgument(argument);
        return null;
    }

    @Override
    public final Void visitExpression(@NotNull JetExpression expression, Void data) {
        visitExpression(expression);
        return null;
    }

    @Override
    public final Void visitLoopExpression(@NotNull JetLoopExpression loopExpression, Void data) {
        visitLoopExpression(loopExpression);
        return null;
    }

    @Override
    public final Void visitConstantExpression(@NotNull JetConstantExpression expression, Void data) {
        visitConstantExpression(expression);
        return null;
    }

    @Override
    public final Void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression, Void data) {
        visitSimpleNameExpression(expression);
        return null;
    }

    @Override
    public final Void visitReferenceExpression(@NotNull JetReferenceExpression expression, Void data) {
        visitReferenceExpression(expression);
        return null;
    }

    @Override
    public final Void visitLabeledExpression(@NotNull JetLabeledExpression expression, Void data) {
        visitLabeledExpression(expression);
        return null;
    }

    @Override
    public final Void visitPrefixExpression(@NotNull JetPrefixExpression expression, Void data) {
        visitPrefixExpression(expression);
        return null;
    }

    @Override
    public final Void visitPostfixExpression(@NotNull JetPostfixExpression expression, Void data) {
        visitPostfixExpression(expression);
        return null;
    }

    @Override
    public final Void visitUnaryExpression(@NotNull JetUnaryExpression expression, Void data) {
        visitUnaryExpression(expression);
        return null;
    }

    @Override
    public final Void visitBinaryExpression(@NotNull JetBinaryExpression expression, Void data) {
        visitBinaryExpression(expression);
        return null;
    }

    @Override
    public final Void visitReturnExpression(@NotNull JetReturnExpression expression, Void data) {
        visitReturnExpression(expression);
        return null;
    }

    @Override
    public final Void visitExpressionWithLabel(@NotNull JetExpressionWithLabel expression, Void data) {
        visitExpressionWithLabel(expression);
        return null;
    }

    @Override
    public final Void visitThrowExpression(@NotNull JetThrowExpression expression, Void data) {
        visitThrowExpression(expression);
        return null;
    }

    @Override
    public final Void visitBreakExpression(@NotNull JetBreakExpression expression, Void data) {
        visitBreakExpression(expression);
        return null;
    }

    @Override
    public final Void visitContinueExpression(@NotNull JetContinueExpression expression, Void data) {
        visitContinueExpression(expression);
        return null;
    }

    @Override
    public final Void visitIfExpression(@NotNull JetIfExpression expression, Void data) {
        visitIfExpression(expression);
        return null;
    }

    @Override
    public final Void visitWhenExpression(@NotNull JetWhenExpression expression, Void data) {
        visitWhenExpression(expression);
        return null;
    }

    @Override
    public final Void visitTryExpression(@NotNull JetTryExpression expression, Void data) {
        visitTryExpression(expression);
        return null;
    }

    @Override
    public final Void visitForExpression(@NotNull JetForExpression expression, Void data) {
        visitForExpression(expression);
        return null;
    }

    @Override
    public final Void visitWhileExpression(@NotNull JetWhileExpression expression, Void data) {
        visitWhileExpression(expression);
        return null;
    }

    @Override
    public final Void visitDoWhileExpression(@NotNull JetDoWhileExpression expression, Void data) {
        visitDoWhileExpression(expression);
        return null;
    }

    @Override
    public final Void visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression, Void data) {
        visitFunctionLiteralExpression(expression);
        return null;
    }

    @Override
    public final Void visitAnnotatedExpression(@NotNull JetAnnotatedExpression expression, Void data) {
        visitAnnotatedExpression(expression);
        return null;
    }

    @Override
    public final Void visitCallExpression(@NotNull JetCallExpression expression, Void data) {
        visitCallExpression(expression);
        return null;
    }

    @Override
    public final Void visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression, Void data) {
        visitArrayAccessExpression(expression);
        return null;
    }

    @Override
    public final Void visitQualifiedExpression(@NotNull JetQualifiedExpression expression, Void data) {
        visitQualifiedExpression(expression);
        return null;
    }

    @Override
    public final Void visitDoubleColonExpression(@NotNull JetDoubleColonExpression expression, Void data) {
        visitDoubleColonExpression(expression);
        return null;
    }

    @Override
    public final Void visitCallableReferenceExpression(@NotNull JetCallableReferenceExpression expression, Void data) {
        visitCallableReferenceExpression(expression);
        return null;
    }

    @Override
    public final Void visitClassLiteralExpression(@NotNull JetClassLiteralExpression expression, Void data) {
        visitClassLiteralExpression(expression);
        return null;
    }

    @Override
    public final Void visitDotQualifiedExpression(@NotNull JetDotQualifiedExpression expression, Void data) {
        visitDotQualifiedExpression(expression);
        return null;
    }

    @Override
    public final Void visitSafeQualifiedExpression(@NotNull JetSafeQualifiedExpression expression, Void data) {
        visitSafeQualifiedExpression(expression);
        return null;
    }

    @Override
    public final Void visitObjectLiteralExpression(@NotNull JetObjectLiteralExpression expression, Void data) {
        visitObjectLiteralExpression(expression);
        return null;
    }

    @Override
    public final Void visitRootPackageExpression(@NotNull JetRootPackageExpression expression, Void data) {
        visitRootPackageExpression(expression);
        return null;
    }

    @Override
    public final Void visitBlockExpression(@NotNull JetBlockExpression expression, Void data) {
        visitBlockExpression(expression);
        return null;
    }

    @Override
    public final Void visitCatchSection(@NotNull JetCatchClause catchClause, Void data) {
        visitCatchSection(catchClause);
        return null;
    }

    @Override
    public final Void visitFinallySection(@NotNull JetFinallySection finallySection, Void data) {
        visitFinallySection(finallySection);
        return null;
    }

    @Override
    public final Void visitTypeArgumentList(@NotNull JetTypeArgumentList typeArgumentList, Void data) {
        visitTypeArgumentList(typeArgumentList);
        return null;
    }

    @Override
    public final Void visitThisExpression(@NotNull JetThisExpression expression, Void data) {
        visitThisExpression(expression);
        return null;
    }

    @Override
    public final Void visitSuperExpression(@NotNull JetSuperExpression expression, Void data) {
        visitSuperExpression(expression);
        return null;
    }

    @Override
    public final Void visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression, Void data) {
        visitParenthesizedExpression(expression);
        return null;
    }

    @Override
    public final Void visitInitializerList(@NotNull JetInitializerList list, Void data) {
        visitInitializerList(list);
        return null;
    }

    @Override
    public final Void visitAnonymousInitializer(@NotNull JetClassInitializer initializer, Void data) {
        visitAnonymousInitializer(initializer);
        return null;
    }

    @Override
    public final Void visitPropertyAccessor(@NotNull JetPropertyAccessor accessor, Void data) {
        visitPropertyAccessor(accessor);
        return null;
    }

    @Override
    public final Void visitTypeConstraintList(@NotNull JetTypeConstraintList list, Void data) {
        visitTypeConstraintList(list);
        return null;
    }

    @Override
    public final Void visitTypeConstraint(@NotNull JetTypeConstraint constraint, Void data) {
        visitTypeConstraint(constraint);
        return null;
    }

    @Override
    public final Void visitUserType(@NotNull JetUserType type, Void data) {
        visitUserType(type);
        return null;
    }

    @Override
    public Void visitDynamicType(@NotNull JetDynamicType type, Void data) {
        visitDynamicType(type);
        return null;
    }

    @Override
    public final Void visitFunctionType(@NotNull JetFunctionType type, Void data) {
        visitFunctionType(type);
        return null;
    }

    @Override
    public final Void visitSelfType(@NotNull JetSelfType type, Void data) {
        visitSelfType(type);
        return null;
    }

    @Override
    public final Void visitBinaryWithTypeRHSExpression(@NotNull JetBinaryExpressionWithTypeRHS expression, Void data) {
        visitBinaryWithTypeRHSExpression(expression);
        return null;
    }

    @Override
    public final Void visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression, Void data) {
        visitStringTemplateExpression(expression);
        return null;
    }

    @Override
    public final Void visitNamedDeclaration(@NotNull JetNamedDeclaration declaration, Void data) {
        visitNamedDeclaration(declaration);
        return null;
    }

    @Override
    public final Void visitNullableType(@NotNull JetNullableType nullableType, Void data) {
        visitNullableType(nullableType);
        return null;
    }

    @Override
    public final Void visitTypeProjection(@NotNull JetTypeProjection typeProjection, Void data) {
        visitTypeProjection(typeProjection);
        return null;
    }

    @Override
    public final Void visitWhenEntry(@NotNull JetWhenEntry jetWhenEntry, Void data) {
        visitWhenEntry(jetWhenEntry);
        return null;
    }

    @Override
    public final Void visitIsExpression(@NotNull JetIsExpression expression, Void data) {
        visitIsExpression(expression);
        return null;
    }

    @Override
    public final Void visitWhenConditionIsPattern(@NotNull JetWhenConditionIsPattern condition, Void data) {
        visitWhenConditionIsPattern(condition);
        return null;
    }

    @Override
    public final Void visitWhenConditionInRange(@NotNull JetWhenConditionInRange condition, Void data) {
        visitWhenConditionInRange(condition);
        return null;
    }

    @Override
    public final Void visitWhenConditionWithExpression(@NotNull JetWhenConditionWithExpression condition, Void data) {
        visitWhenConditionWithExpression(condition);
        return null;
    }

    @Override
    public final Void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, Void data) {
        visitObjectDeclaration(declaration);
        return null;
    }

    @Override
    public final Void visitObjectDeclarationName(@NotNull JetObjectDeclarationName declarationName, Void data) {
        visitObjectDeclarationName(declarationName);
        return null;
    }

    @Override
    public final Void visitStringTemplateEntry(@NotNull JetStringTemplateEntry entry, Void data) {
        visitStringTemplateEntry(entry);
        return null;
    }

    @Override
    public final Void visitStringTemplateEntryWithExpression(@NotNull JetStringTemplateEntryWithExpression entry, Void data) {
        visitStringTemplateEntryWithExpression(entry);
        return null;
    }

    @Override
    public final Void visitBlockStringTemplateEntry(@NotNull JetBlockStringTemplateEntry entry, Void data) {
        visitBlockStringTemplateEntry(entry);
        return null;
    }

    @Override
    public final Void visitSimpleNameStringTemplateEntry(@NotNull JetSimpleNameStringTemplateEntry entry, Void data) {
        visitSimpleNameStringTemplateEntry(entry);
        return null;
    }

    @Override
    public final Void visitLiteralStringTemplateEntry(@NotNull JetLiteralStringTemplateEntry entry, Void data) {
        visitLiteralStringTemplateEntry(entry);
        return null;
    }

    @Override
    public final Void visitEscapeStringTemplateEntry(@NotNull JetEscapeStringTemplateEntry entry, Void data) {
        visitEscapeStringTemplateEntry(entry);
        return null;
    }

    @Override
    public Void visitPackageDirective(@NotNull JetPackageDirective directive, Void data) {
        visitPackageDirective(directive);
        return null;
    }
}
