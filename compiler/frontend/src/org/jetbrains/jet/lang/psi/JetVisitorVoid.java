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

public class JetVisitorVoid extends PsiElementVisitor {
    public void visitJetElement(JetElement element) {
        visitElement(element);
    }

    public void visitDeclaration(JetDeclaration dcl) {
        visitExpression(dcl);
    }

    public void visitClass(JetClass klass) {
        visitNamedDeclaration(klass);
    }

    public void visitClassObject(JetClassObject classObject) {
        visitDeclaration(classObject);
    }

    public void visitNamedFunction(JetNamedFunction function) {
        visitNamedDeclaration(function);
    }

    public void visitProperty(JetProperty property) {
        visitNamedDeclaration(property);
    }

    public void visitTypedef(JetTypedef typedef) {
        visitNamedDeclaration(typedef);
    }

    public void visitJetFile(JetFile file) {
        visitFile(file);
    }

    public void visitScript(JetScript script) {
        visitDeclaration(script);
    }

    public void visitImportDirective(JetImportDirective importDirective) {
        visitJetElement(importDirective);
    }

    public void visitClassBody(JetClassBody classBody) {
        visitJetElement(classBody);
    }

    public void visitModifierList(JetModifierList list) {
        visitJetElement(list);
    }

    public void visitAnnotation(JetAnnotation annotation) {
        visitJetElement(annotation);
    }

    public void visitAnnotationEntry(JetAnnotationEntry annotationEntry) {
        visitJetElement(annotationEntry);
    }

    public void visitTypeParameterList(JetTypeParameterList list) {
        visitJetElement(list);
    }

    public void visitTypeParameter(JetTypeParameter parameter) {
        visitNamedDeclaration(parameter);
    }

    public void visitEnumEntry(JetEnumEntry enumEntry) {
        visitClass(enumEntry);
    }

    public void visitParameterList(JetParameterList list) {
        visitJetElement(list);
    }

    public void visitParameter(JetParameter parameter) {
        visitNamedDeclaration(parameter);
    }

    public void visitDelegationSpecifierList(JetDelegationSpecifierList list) {
        visitJetElement(list);
    }

    public void visitDelegationSpecifier(JetDelegationSpecifier specifier) {
        visitJetElement(specifier);
    }

    public void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier) {
        visitDelegationSpecifier(specifier);
    }

    public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
        visitDelegationSpecifier(call);
    }

    public void visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier) {
        visitDelegationSpecifier(specifier);
    }

    public void visitDelegationToThisCall(JetDelegatorToThisCall thisCall) {
        visitDelegationSpecifier(thisCall);
    }

    public void visitPropertyDelegate(JetPropertyDelegate delegate) {
        visitJetElement(delegate);
    }

    public void visitTypeReference(JetTypeReference typeReference) {
        visitJetElement(typeReference);
    }

    public void visitValueArgumentList(JetValueArgumentList list) {
        visitJetElement(list);
    }

    public void visitArgument(JetValueArgument argument) {
        visitJetElement(argument);
    }

    public void visitExpression(JetExpression expression) {
        visitJetElement(expression);
    }

    public void visitLoopExpression(JetLoopExpression loopExpression) {
        visitExpression(loopExpression);
    }

    public void visitConstantExpression(JetConstantExpression expression) {
        visitExpression(expression);
    }

    public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
        visitReferenceExpression(expression);
    }

    public void visitReferenceExpression(JetReferenceExpression expression) {
        visitExpression(expression);
    }

    public void visitPrefixExpression(JetPrefixExpression expression) {
        visitUnaryExpression(expression);
    }

    public void visitPostfixExpression(JetPostfixExpression expression) {
        visitUnaryExpression(expression);
    }

    public void visitUnaryExpression(JetUnaryExpression expression) {
        visitExpression(expression);
    }

    public void visitBinaryExpression(JetBinaryExpression expression) {
        visitExpression(expression);
    }

    //    public void visitNewExpression(JetNewExpression expression) {
//        visitExpression(expression);
//    }
//
    public void visitReturnExpression(JetReturnExpression expression) {
        visitLabelQualifiedExpression(expression);
    }

    public void visitLabelQualifiedExpression(JetLabelQualifiedExpression expression) {
        visitExpression(expression);
    }

    public void visitThrowExpression(JetThrowExpression expression) {
        visitExpression(expression);
    }

    public void visitBreakExpression(JetBreakExpression expression) {
        visitLabelQualifiedExpression(expression);
    }

    public void visitContinueExpression(JetContinueExpression expression) {
        visitLabelQualifiedExpression(expression);
    }

    public void visitIfExpression(JetIfExpression expression) {
        visitExpression(expression);
    }

    public void visitWhenExpression(JetWhenExpression expression) {
        visitExpression(expression);
    }

    public void visitTryExpression(JetTryExpression expression) {
        visitExpression(expression);
    }

    public void visitForExpression(JetForExpression expression) {
        visitLoopExpression(expression);
    }

    public void visitWhileExpression(JetWhileExpression expression) {
        visitLoopExpression(expression);
    }

    public void visitDoWhileExpression(JetDoWhileExpression expression) {
        visitLoopExpression(expression);
    }

    public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
        visitExpression(expression);
    }

    public void visitAnnotatedExpression(JetAnnotatedExpression expression) {
        visitExpression(expression);
    }

    public void visitCallExpression(JetCallExpression expression) {
        visitReferenceExpression(expression);
    }

    public void visitArrayAccessExpression(JetArrayAccessExpression expression) {
        visitReferenceExpression(expression);
    }

    public void visitQualifiedExpression(JetQualifiedExpression expression) {
        visitExpression(expression);
    }

    public void visitCallableReferenceExpression(JetCallableReferenceExpression expression) {
        visitExpression(expression);
    }

    public void visitDotQualifiedExpression(JetDotQualifiedExpression expression) {
        visitQualifiedExpression(expression);
    }

    public void visitSafeQualifiedExpression(JetSafeQualifiedExpression expression) {
        visitQualifiedExpression(expression);
    }

    public void visitObjectLiteralExpression(JetObjectLiteralExpression expression) {
        visitExpression(expression);
    }

    public void visitRootNamespaceExpression(JetRootNamespaceExpression expression) {
        visitExpression(expression);
    }

    public void visitBlockExpression(JetBlockExpression expression) {
        visitExpression(expression);
    }

    public void visitIdeTemplate(JetIdeTemplate expression) {
    }

    public void visitCatchSection(JetCatchClause catchClause) {
        visitJetElement(catchClause);
    }

    public void visitFinallySection(JetFinallySection finallySection) {
        visitJetElement(finallySection);
    }

    public void visitTypeArgumentList(JetTypeArgumentList typeArgumentList) {
        visitJetElement(typeArgumentList);
    }

    public void visitThisExpression(JetThisExpression expression) {
        visitLabelQualifiedExpression(expression);
    }

    public void visitSuperExpression(JetSuperExpression expression) {
        visitLabelQualifiedExpression(expression);
    }

    public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
        visitExpression(expression);
    }

    public void visitInitializerList(JetInitializerList list) {
        visitJetElement(list);
    }

    public void visitAnonymousInitializer(JetClassInitializer initializer) {
        visitDeclaration(initializer);
    }

    public void visitPropertyAccessor(JetPropertyAccessor accessor) {
        visitDeclaration(accessor);
    }

    public void visitTypeConstraintList(JetTypeConstraintList list) {
        visitJetElement(list);
    }

    public void visitTypeConstraint(JetTypeConstraint constraint) {
        visitJetElement(constraint);
    }

    private void visitTypeElement(JetTypeElement type) {
        visitJetElement(type);
    }

    public void visitUserType(JetUserType type) {
        visitTypeElement(type);
    }

    public void visitFunctionType(JetFunctionType type) {
        visitTypeElement(type);
    }

    public void visitSelfType(JetSelfType type) {
        visitTypeElement(type);
    }

    public void visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression) {
        visitExpression(expression);
    }

    public void visitStringTemplateExpression(JetStringTemplateExpression expression) {
        visitExpression(expression);
    }

    public void visitNamedDeclaration(JetNamedDeclaration declaration) {
        visitDeclaration(declaration);
    }

    public void visitNullableType(JetNullableType nullableType) {
        visitTypeElement(nullableType);
    }

    public void visitTypeProjection(JetTypeProjection typeProjection) {
        visitJetElement(typeProjection);
    }

    public void visitWhenEntry(JetWhenEntry jetWhenEntry) {
        visitJetElement(jetWhenEntry);
    }

    public void visitIsExpression(JetIsExpression expression) {
        visitExpression(expression);
    }

    public void visitWhenConditionIsPattern(JetWhenConditionIsPattern condition) {
        visitJetElement(condition);
    }

    public void visitWhenConditionInRange(JetWhenConditionInRange condition) {
        visitJetElement(condition);
    }

    public void visitWhenConditionWithExpression(JetWhenConditionWithExpression condition) {
        visitJetElement(condition);
    }

    public void visitObjectDeclaration(JetObjectDeclaration declaration) {
        visitNamedDeclaration(declaration);
    }

    public void visitObjectDeclarationName(JetObjectDeclarationName declaration) {
        visitNamedDeclaration(declaration);
    }

    public void visitStringTemplateEntry(JetStringTemplateEntry entry) {
        visitJetElement(entry);
    }

    public void visitStringTemplateEntryWithExpression(JetStringTemplateEntryWithExpression entry) {
        visitStringTemplateEntry(entry);
    }

    public void visitBlockStringTemplateEntry(JetBlockStringTemplateEntry entry) {
        visitStringTemplateEntryWithExpression(entry);
    }

    public void visitSimpleNameStringTemplateEntry(JetSimpleNameStringTemplateEntry entry) {
        visitStringTemplateEntryWithExpression(entry);
    }

    public void visitLiteralStringTemplateEntry(JetLiteralStringTemplateEntry entry) {
        visitStringTemplateEntry(entry);
    }

    public void visitEscapeStringTemplateEntry(JetEscapeStringTemplateEntry entry) {
        visitStringTemplateEntry(entry);
    }

    public void visitMultiDeclaration(JetMultiDeclaration declaration) {
        visitDeclaration(declaration);
    }

    public void visitMultiDeclarationEntry(JetMultiDeclarationEntry entry) {
        visitNamedDeclaration(entry);
    }
}
