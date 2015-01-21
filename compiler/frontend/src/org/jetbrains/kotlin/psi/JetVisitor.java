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

public class JetVisitor<R, D> extends PsiElementVisitor {
    public R visitJetElement(@NotNull JetElement element, D data) {
        visitElement(element);
        return null;
    }

    public R visitDeclaration(@NotNull JetDeclaration dcl, D data) {
        return visitExpression(dcl, data);
    }

    public R visitClass(@NotNull JetClass klass, D data) {
        return visitNamedDeclaration(klass, data);
    }

    public R visitSecondaryConstructor(@NotNull JetSecondaryConstructor constructor, D data) {
        return visitJetElement(constructor, data);
    }

    public R visitNamedFunction(@NotNull JetNamedFunction function, D data) {
        return visitNamedDeclaration(function, data);
    }

    public R visitProperty(@NotNull JetProperty property, D data) {
        return visitNamedDeclaration(property, data);
    }

    public R visitMultiDeclaration(@NotNull JetMultiDeclaration multiDeclaration, D data) {
        return visitDeclaration(multiDeclaration, data);
    }

    public R visitMultiDeclarationEntry(@NotNull JetMultiDeclarationEntry multiDeclarationEntry, D data) {
        return visitNamedDeclaration(multiDeclarationEntry, data);
    }

    public R visitTypedef(@NotNull JetTypedef typedef, D data) {
        return visitNamedDeclaration(typedef, data);
    }

    public R visitJetFile(@NotNull JetFile file, D data) {
        visitFile(file);
        return null;
    }

    public R visitScript(@NotNull JetScript script, D data) {
        return visitDeclaration(script, data);
    }

    public R visitImportDirective(@NotNull JetImportDirective importDirective, D data) {
        return visitJetElement(importDirective, data);
    }

    public R visitImportList(@NotNull JetImportList importList, D data) {
        return visitJetElement(importList, data);
    }

    public R visitFileAnnotationList(@NotNull JetFileAnnotationList fileAnnotationList, D data) {
        return visitJetElement(fileAnnotationList, data);
    }

    public R visitClassBody(@NotNull JetClassBody classBody, D data) {
        return visitJetElement(classBody, data);
    }

    public R visitModifierList(@NotNull JetModifierList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitAnnotation(@NotNull JetAnnotation annotation, D data) {
        return visitJetElement(annotation, data);
    }

    public R visitAnnotationEntry(@NotNull JetAnnotationEntry annotationEntry, D data) {
        return visitJetElement(annotationEntry, data);
    }

    public R visitConstructorCalleeExpression(@NotNull JetConstructorCalleeExpression constructorCalleeExpression, D data) {
        return visitJetElement(constructorCalleeExpression, data);
    }

    public R visitTypeParameterList(@NotNull JetTypeParameterList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitTypeParameter(@NotNull JetTypeParameter parameter, D data) {
        return visitNamedDeclaration(parameter, data);
    }

    public R visitEnumEntry(@NotNull JetEnumEntry enumEntry, D data) {
        return visitClass(enumEntry, data);
    }

    public R visitParameterList(@NotNull JetParameterList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitParameter(@NotNull JetParameter parameter, D data) {
        return visitNamedDeclaration(parameter, data);
    }

    public R visitDelegationSpecifierList(@NotNull JetDelegationSpecifierList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitDelegationSpecifier(@NotNull JetDelegationSpecifier specifier, D data) {
        return visitJetElement(specifier, data);
    }

    public R visitDelegationByExpressionSpecifier(@NotNull JetDelegatorByExpressionSpecifier specifier, D data) {
        return visitDelegationSpecifier(specifier, data);
    }

    public R visitDelegationToSuperCallSpecifier(@NotNull JetDelegatorToSuperCall call, D data) {
        return visitDelegationSpecifier(call, data);
    }

    public R visitDelegationToSuperClassSpecifier(@NotNull JetDelegatorToSuperClass specifier, D data) {
        return visitDelegationSpecifier(specifier, data);
    }

    public R visitConstructorDelegationCall(@NotNull JetConstructorDelegationCall call, D data) {
        return visitJetElement(call, data);
    }

    public R visitPropertyDelegate(@NotNull JetPropertyDelegate delegate, D data) {
        return visitJetElement(delegate, data);
    }

    public R visitTypeReference(@NotNull JetTypeReference typeReference, D data) {
        return visitJetElement(typeReference, data);
    }

    public R visitValueArgumentList(@NotNull JetValueArgumentList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitArgument(@NotNull JetValueArgument argument, D data) {
        return visitJetElement(argument, data);
    }

    public R visitExpression(@NotNull JetExpression expression, D data) {
        return visitJetElement(expression, data);
    }

    public R visitLoopExpression(@NotNull JetLoopExpression loopExpression, D data) {
        return visitExpression(loopExpression, data);
    }

    public R visitConstantExpression(@NotNull JetConstantExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression, D data) {
        return visitReferenceExpression(expression, data);
    }

    public R visitReferenceExpression(@NotNull JetReferenceExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitLabeledExpression(@NotNull JetLabeledExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitPrefixExpression(@NotNull JetPrefixExpression expression, D data) {
        return visitUnaryExpression(expression, data);
    }

    public R visitPostfixExpression(@NotNull JetPostfixExpression expression, D data) {
        return visitUnaryExpression(expression, data);
    }

    public R visitUnaryExpression(@NotNull JetUnaryExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitBinaryExpression(@NotNull JetBinaryExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitReturnExpression(@NotNull JetReturnExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitExpressionWithLabel(@NotNull JetExpressionWithLabel expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitThrowExpression(@NotNull JetThrowExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitBreakExpression(@NotNull JetBreakExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitContinueExpression(@NotNull JetContinueExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitIfExpression(@NotNull JetIfExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitWhenExpression(@NotNull JetWhenExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitTryExpression(@NotNull JetTryExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitForExpression(@NotNull JetForExpression expression, D data) {
        return visitLoopExpression(expression, data);
    }

    public R visitWhileExpression(@NotNull JetWhileExpression expression, D data) {
        return visitLoopExpression(expression, data);
    }

    public R visitDoWhileExpression(@NotNull JetDoWhileExpression expression, D data) {
        return visitLoopExpression(expression, data);
    }

    public R visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitAnnotatedExpression(@NotNull JetAnnotatedExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitCallExpression(@NotNull JetCallExpression expression, D data) {
        return visitReferenceExpression(expression, data);
    }

    public R visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression, D data) {
        return visitReferenceExpression(expression, data);
    }

    public R visitQualifiedExpression(@NotNull JetQualifiedExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitDoubleColonExpression(@NotNull JetDoubleColonExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitCallableReferenceExpression(@NotNull JetCallableReferenceExpression expression, D data) {
        return visitDoubleColonExpression(expression, data);
    }

    public R visitClassLiteralExpression(@NotNull JetClassLiteralExpression expression, D data) {
        return visitDoubleColonExpression(expression, data);
    }

    public R visitDotQualifiedExpression(@NotNull JetDotQualifiedExpression expression, D data) {
        return visitQualifiedExpression(expression, data);
    }

    public R visitSafeQualifiedExpression(@NotNull JetSafeQualifiedExpression expression, D data) {
        return visitQualifiedExpression(expression, data);
    }

    public R visitObjectLiteralExpression(@NotNull JetObjectLiteralExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitRootPackageExpression(@NotNull JetRootPackageExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitBlockExpression(@NotNull JetBlockExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitCatchSection(@NotNull JetCatchClause catchClause, D data) {
        return visitJetElement(catchClause, data);
    }

    public R visitFinallySection(@NotNull JetFinallySection finallySection, D data) {
        return visitJetElement(finallySection, data);
    }

    public R visitTypeArgumentList(@NotNull JetTypeArgumentList typeArgumentList, D data) {
        return visitJetElement(typeArgumentList, data);
    }

    public R visitThisExpression(@NotNull JetThisExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitSuperExpression(@NotNull JetSuperExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitInitializerList(@NotNull JetInitializerList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitAnonymousInitializer(@NotNull JetClassInitializer initializer, D data) {
        return visitDeclaration(initializer, data);
    }

    public R visitPropertyAccessor(@NotNull JetPropertyAccessor accessor, D data) {
        return visitDeclaration(accessor, data);
    }

    public R visitTypeConstraintList(@NotNull JetTypeConstraintList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitTypeConstraint(@NotNull JetTypeConstraint constraint, D data) {
        return visitJetElement(constraint, data);
    }

    private R visitTypeElement(@NotNull JetTypeElement type, D data) {
        return visitJetElement(type, data);
    }

    public R visitUserType(@NotNull JetUserType type, D data) {
        return visitTypeElement(type, data);
    }

    public R visitDynamicType(@NotNull JetDynamicType type, D data) {
        return visitTypeElement(type, data);
    }

    public R visitFunctionType(@NotNull JetFunctionType type, D data) {
        return visitTypeElement(type, data);
    }

    public R visitSelfType(@NotNull JetSelfType type, D data) {
        return visitTypeElement(type, data);
    }

    public R visitBinaryWithTypeRHSExpression(@NotNull JetBinaryExpressionWithTypeRHS expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitNamedDeclaration(@NotNull JetNamedDeclaration declaration, D data) {
        return visitDeclaration(declaration, data);
    }

    public R visitNullableType(@NotNull JetNullableType nullableType, D data) {
        return visitTypeElement(nullableType, data);
    }

    public R visitTypeProjection(@NotNull JetTypeProjection typeProjection, D data) {
        return visitJetElement(typeProjection, data);
    }

    public R visitWhenEntry(@NotNull JetWhenEntry jetWhenEntry, D data) {
        return visitJetElement(jetWhenEntry, data);
    }

    public R visitIsExpression(@NotNull JetIsExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitWhenConditionIsPattern(@NotNull JetWhenConditionIsPattern condition, D data) {
        return visitJetElement(condition, data);
    }

    public R visitWhenConditionInRange(@NotNull JetWhenConditionInRange condition, D data) {
        return visitJetElement(condition, data);
    }

    public R visitWhenConditionWithExpression(@NotNull JetWhenConditionWithExpression condition, D data) {
        return visitJetElement(condition, data);
    }

    public R visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, D data) {
        return visitNamedDeclaration(declaration, data);
    }

    public R visitObjectDeclarationName(@NotNull JetObjectDeclarationName declarationName, D data) {
        return visitExpression(declarationName, data);
    }

    public R visitStringTemplateEntry(@NotNull JetStringTemplateEntry entry, D data) {
        return visitJetElement(entry, data);
    }

    public R visitStringTemplateEntryWithExpression(@NotNull JetStringTemplateEntryWithExpression entry, D data) {
        return visitStringTemplateEntry(entry, data);
    }

    public R visitBlockStringTemplateEntry(@NotNull JetBlockStringTemplateEntry entry, D data) {
        return visitStringTemplateEntryWithExpression(entry, data);
    }

    public R visitSimpleNameStringTemplateEntry(@NotNull JetSimpleNameStringTemplateEntry entry, D data) {
        return visitStringTemplateEntryWithExpression(entry, data);
    }

    public R visitLiteralStringTemplateEntry(@NotNull JetLiteralStringTemplateEntry entry, D data) {
        return visitStringTemplateEntry(entry, data);
    }

    public R visitEscapeStringTemplateEntry(@NotNull JetEscapeStringTemplateEntry entry, D data) {
        return visitStringTemplateEntry(entry, data);
    }

    public R visitPackageDirective(@NotNull JetPackageDirective directive, D data) {
        return visitExpression(directive, data);
    }
}
