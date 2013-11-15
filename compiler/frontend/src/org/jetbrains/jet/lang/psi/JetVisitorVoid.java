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

package org.jetbrains.jet.lang.psi;

import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class JetVisitorVoid extends PsiElementVisitor {
    public void visitJetElement(@NotNull JetElement element) {
        visitElement(element);
    }

    public void visitDeclaration(@NotNull JetDeclaration dcl) {
        visitExpression(dcl);
    }

    public void visitClass(@NotNull JetClass klass) {
        visitNamedDeclaration(klass);
    }

    public void visitClassObject(@NotNull JetClassObject classObject) {
        visitDeclaration(classObject);
    }

    public void visitNamedFunction(@NotNull JetNamedFunction function) {
        visitNamedDeclaration(function);
    }

    public void visitProperty(@NotNull JetProperty property) {
        visitNamedDeclaration(property);
    }

    public void visitTypedef(@NotNull JetTypedef typedef) {
        visitNamedDeclaration(typedef);
    }

    public void visitJetFile(@NotNull JetFile file) {
        visitFile(file);
    }

    public void visitScript(@NotNull JetScript script) {
        visitDeclaration(script);
    }

    public void visitImportDirective(@NotNull JetImportDirective importDirective) {
        visitJetElement(importDirective);
    }

    public void visitImportList(@NotNull JetImportList importList) {
        visitJetElement(importList);
    }

    public void visitClassBody(@NotNull JetClassBody classBody) {
        visitJetElement(classBody);
    }

    public void visitModifierList(@NotNull JetModifierList list) {
        visitJetElement(list);
    }

    public void visitAnnotation(@NotNull JetAnnotation annotation) {
        visitJetElement(annotation);
    }

    public void visitAnnotationEntry(@NotNull JetAnnotationEntry annotationEntry) {
        visitJetElement(annotationEntry);
    }

    public void visitTypeParameterList(@NotNull JetTypeParameterList list) {
        visitJetElement(list);
    }

    public void visitTypeParameter(@NotNull JetTypeParameter parameter) {
        visitNamedDeclaration(parameter);
    }

    public void visitEnumEntry(@NotNull JetEnumEntry enumEntry) {
        visitClass(enumEntry);
    }

    public void visitParameterList(@NotNull JetParameterList list) {
        visitJetElement(list);
    }

    public void visitParameter(@NotNull JetParameter parameter) {
        visitNamedDeclaration(parameter);
    }

    public void visitDelegationSpecifierList(@NotNull JetDelegationSpecifierList list) {
        visitJetElement(list);
    }

    public void visitDelegationSpecifier(@NotNull JetDelegationSpecifier specifier) {
        visitJetElement(specifier);
    }

    public void visitDelegationByExpressionSpecifier(@NotNull JetDelegatorByExpressionSpecifier specifier) {
        visitDelegationSpecifier(specifier);
    }

    public void visitDelegationToSuperCallSpecifier(@NotNull JetDelegatorToSuperCall call) {
        visitDelegationSpecifier(call);
    }

    public void visitDelegationToSuperClassSpecifier(@NotNull JetDelegatorToSuperClass specifier) {
        visitDelegationSpecifier(specifier);
    }

    public void visitDelegationToThisCall(@NotNull JetDelegatorToThisCall thisCall) {
        visitDelegationSpecifier(thisCall);
    }

    public void visitPropertyDelegate(@NotNull JetPropertyDelegate delegate) {
        visitJetElement(delegate);
    }

    public void visitTypeReference(@NotNull JetTypeReference typeReference) {
        visitJetElement(typeReference);
    }

    public void visitValueArgumentList(@NotNull JetValueArgumentList list) {
        visitJetElement(list);
    }

    public void visitArgument(@NotNull JetValueArgument argument) {
        visitJetElement(argument);
    }

    public void visitExpression(@NotNull JetExpression expression) {
        visitJetElement(expression);
    }

    public void visitLoopExpression(@NotNull JetLoopExpression loopExpression) {
        visitExpression(loopExpression);
    }

    public void visitConstantExpression(@NotNull JetConstantExpression expression) {
        visitExpression(expression);
    }

    public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression) {
        visitReferenceExpression(expression);
    }

    public void visitReferenceExpression(@NotNull JetReferenceExpression expression) {
        visitExpression(expression);
    }

    public void visitPrefixExpression(@NotNull JetPrefixExpression expression) {
        visitUnaryExpression(expression);
    }

    public void visitPostfixExpression(@NotNull JetPostfixExpression expression) {
        visitUnaryExpression(expression);
    }

    public void visitUnaryExpression(@NotNull JetUnaryExpression expression) {
        visitExpression(expression);
    }

    public void visitBinaryExpression(@NotNull JetBinaryExpression expression) {
        visitExpression(expression);
    }

    //    public void visitNewExpression(JetNewExpression expression) {
//        visitExpression(expression);
//    }
//
    public void visitReturnExpression(@NotNull JetReturnExpression expression) {
        visitLabelQualifiedExpression(expression);
    }

    public void visitLabelQualifiedExpression(@NotNull JetLabelQualifiedExpression expression) {
        visitExpression(expression);
    }

    public void visitThrowExpression(@NotNull JetThrowExpression expression) {
        visitExpression(expression);
    }

    public void visitBreakExpression(@NotNull JetBreakExpression expression) {
        visitLabelQualifiedExpression(expression);
    }

    public void visitContinueExpression(@NotNull JetContinueExpression expression) {
        visitLabelQualifiedExpression(expression);
    }

    public void visitIfExpression(@NotNull JetIfExpression expression) {
        visitExpression(expression);
    }

    public void visitWhenExpression(@NotNull JetWhenExpression expression) {
        visitExpression(expression);
    }

    public void visitTryExpression(@NotNull JetTryExpression expression) {
        visitExpression(expression);
    }

    public void visitForExpression(@NotNull JetForExpression expression) {
        visitLoopExpression(expression);
    }

    public void visitWhileExpression(@NotNull JetWhileExpression expression) {
        visitLoopExpression(expression);
    }

    public void visitDoWhileExpression(@NotNull JetDoWhileExpression expression) {
        visitLoopExpression(expression);
    }

    public void visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression) {
        visitExpression(expression);
    }

    public void visitAnnotatedExpression(@NotNull JetAnnotatedExpression expression) {
        visitExpression(expression);
    }

    public void visitCallExpression(@NotNull JetCallExpression expression) {
        visitReferenceExpression(expression);
    }

    public void visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression) {
        visitReferenceExpression(expression);
    }

    public void visitQualifiedExpression(@NotNull JetQualifiedExpression expression) {
        visitExpression(expression);
    }

    public void visitCallableReferenceExpression(@NotNull JetCallableReferenceExpression expression) {
        visitExpression(expression);
    }

    public void visitDotQualifiedExpression(@NotNull JetDotQualifiedExpression expression) {
        visitQualifiedExpression(expression);
    }

    public void visitSafeQualifiedExpression(@NotNull JetSafeQualifiedExpression expression) {
        visitQualifiedExpression(expression);
    }

    public void visitObjectLiteralExpression(@NotNull JetObjectLiteralExpression expression) {
        visitExpression(expression);
    }

    public void visitRootNamespaceExpression(@NotNull JetRootNamespaceExpression expression) {
        visitExpression(expression);
    }

    public void visitBlockExpression(@NotNull JetBlockExpression expression) {
        visitExpression(expression);
    }

    public void visitCatchSection(@NotNull JetCatchClause catchClause) {
        visitJetElement(catchClause);
    }

    public void visitFinallySection(@NotNull JetFinallySection finallySection) {
        visitJetElement(finallySection);
    }

    public void visitTypeArgumentList(@NotNull JetTypeArgumentList typeArgumentList) {
        visitJetElement(typeArgumentList);
    }

    public void visitThisExpression(@NotNull JetThisExpression expression) {
        visitLabelQualifiedExpression(expression);
    }

    public void visitSuperExpression(@NotNull JetSuperExpression expression) {
        visitLabelQualifiedExpression(expression);
    }

    public void visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression) {
        visitExpression(expression);
    }

    public void visitInitializerList(@NotNull JetInitializerList list) {
        visitJetElement(list);
    }

    public void visitAnonymousInitializer(@NotNull JetClassInitializer initializer) {
        visitDeclaration(initializer);
    }

    public void visitPropertyAccessor(@NotNull JetPropertyAccessor accessor) {
        visitDeclaration(accessor);
    }

    public void visitTypeConstraintList(@NotNull JetTypeConstraintList list) {
        visitJetElement(list);
    }

    public void visitTypeConstraint(@NotNull JetTypeConstraint constraint) {
        visitJetElement(constraint);
    }

    private void visitTypeElement(@NotNull JetTypeElement type) {
        visitJetElement(type);
    }

    public void visitUserType(@NotNull JetUserType type) {
        visitTypeElement(type);
    }

    public void visitFunctionType(@NotNull JetFunctionType type) {
        visitTypeElement(type);
    }

    public void visitSelfType(@NotNull JetSelfType type) {
        visitTypeElement(type);
    }

    public void visitBinaryWithTypeRHSExpression(@NotNull JetBinaryExpressionWithTypeRHS expression) {
        visitExpression(expression);
    }

    public void visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression) {
        visitExpression(expression);
    }

    public void visitNamedDeclaration(@NotNull JetNamedDeclaration declaration) {
        visitDeclaration(declaration);
    }

    public void visitNullableType(@NotNull JetNullableType nullableType) {
        visitTypeElement(nullableType);
    }

    public void visitTypeProjection(@NotNull JetTypeProjection typeProjection) {
        visitJetElement(typeProjection);
    }

    public void visitWhenEntry(@NotNull JetWhenEntry jetWhenEntry) {
        visitJetElement(jetWhenEntry);
    }

    public void visitIsExpression(@NotNull JetIsExpression expression) {
        visitExpression(expression);
    }

    public void visitWhenConditionIsPattern(@NotNull JetWhenConditionIsPattern condition) {
        visitJetElement(condition);
    }

    public void visitWhenConditionInRange(@NotNull JetWhenConditionInRange condition) {
        visitJetElement(condition);
    }

    public void visitWhenConditionWithExpression(@NotNull JetWhenConditionWithExpression condition) {
        visitJetElement(condition);
    }

    public void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration) {
        visitNamedDeclaration(declaration);
    }

    public void visitObjectDeclarationName(@NotNull JetObjectDeclarationName declaration) {
        visitExpression(declaration);
    }

    public void visitStringTemplateEntry(@NotNull JetStringTemplateEntry entry) {
        visitJetElement(entry);
    }

    public void visitStringTemplateEntryWithExpression(@NotNull JetStringTemplateEntryWithExpression entry) {
        visitStringTemplateEntry(entry);
    }

    public void visitBlockStringTemplateEntry(@NotNull JetBlockStringTemplateEntry entry) {
        visitStringTemplateEntryWithExpression(entry);
    }

    public void visitSimpleNameStringTemplateEntry(@NotNull JetSimpleNameStringTemplateEntry entry) {
        visitStringTemplateEntryWithExpression(entry);
    }

    public void visitLiteralStringTemplateEntry(@NotNull JetLiteralStringTemplateEntry entry) {
        visitStringTemplateEntry(entry);
    }

    public void visitEscapeStringTemplateEntry(@NotNull JetEscapeStringTemplateEntry entry) {
        visitStringTemplateEntry(entry);
    }

    public void visitMultiDeclaration(@NotNull JetMultiDeclaration declaration) {
        visitDeclaration(declaration);
    }

    public void visitMultiDeclarationEntry(@NotNull JetMultiDeclarationEntry entry) {
        visitNamedDeclaration(entry);
    }
}
