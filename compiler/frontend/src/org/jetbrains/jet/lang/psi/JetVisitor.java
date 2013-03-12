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

public class JetVisitor<R, D> extends PsiElementVisitor {
    public R visitJetElement(JetElement element, D data) {
        visitElement(element);
        return null;
    }

    public R visitDeclaration(JetDeclaration dcl, D data) {
        return visitExpression(dcl, data);
    }

    public R visitClass(JetClass klass, D data) {
        return visitNamedDeclaration(klass, data);
    }

    public R visitClassObject(JetClassObject classObject, D data) {
        return visitDeclaration(classObject, data);
    }

    public R visitNamedFunction(JetNamedFunction function, D data) {
        return visitNamedDeclaration(function, data);
    }

    public R visitProperty(JetProperty property, D data) {
        return visitNamedDeclaration(property, data);
    }

    public R visitMultiDeclaration(JetMultiDeclaration multiDeclaration, D data) {
        return visitDeclaration(multiDeclaration, data);
    }

    public R visitMultiDeclarationEntry(JetMultiDeclarationEntry multiDeclarationEntry, D data) {
        return visitNamedDeclaration(multiDeclarationEntry, data);
    }

    public R visitTypedef(JetTypedef typedef, D data) {
        return visitNamedDeclaration(typedef, data);
    }

    public R visitJetFile(JetFile file, D data) {
        visitFile(file);
        return null;
    }

    public R visitScript(JetScript script, D data) {
        return visitDeclaration(script, data);
    }

    public R visitImportDirective(JetImportDirective importDirective, D data) {
        return visitJetElement(importDirective, data);
    }

    public R visitClassBody(JetClassBody classBody, D data) {
        return visitJetElement(classBody, data);
    }

    public R visitModifierList(JetModifierList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitAnnotation(JetAnnotation annotation, D data) {
        return visitJetElement(annotation, data);
    }

    public R visitAnnotationEntry(JetAnnotationEntry annotationEntry, D data) {
        return visitJetElement(annotationEntry, data);
    }

    public R visitTypeParameterList(JetTypeParameterList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitTypeParameter(JetTypeParameter parameter, D data) {
        return visitNamedDeclaration(parameter, data);
    }

    public R visitEnumEntry(JetEnumEntry enumEntry, D data) {
        return visitClass(enumEntry, data);
    }

    public R visitParameterList(JetParameterList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitParameter(JetParameter parameter, D data) {
        return visitNamedDeclaration(parameter, data);
    }

    public R visitDelegationSpecifierList(JetDelegationSpecifierList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitDelegationSpecifier(JetDelegationSpecifier specifier, D data) {
        return visitJetElement(specifier, data);
    }

    public R visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier, D data) {
        return visitDelegationSpecifier(specifier, data);
    }

    public R visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call, D data) {
        return visitDelegationSpecifier(call, data);
    }

    public R visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier, D data) {
        return visitDelegationSpecifier(specifier, data);
    }

    public R visitDelegationToThisCall(JetDelegatorToThisCall thisCall, D data) {
        return visitDelegationSpecifier(thisCall, data);
    }

    public R visitTypeReference(JetTypeReference typeReference, D data) {
        return visitJetElement(typeReference, data);
    }

    public R visitValueArgumentList(JetValueArgumentList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitArgument(JetValueArgument argument, D data) {
        return visitJetElement(argument, data);
    }

    public R visitExpression(JetExpression expression, D data) {
        return visitJetElement(expression, data);
    }

    public R visitLoopExpression(JetLoopExpression loopExpression, D data) {
        return visitExpression(loopExpression, data);
    }

    public R visitConstantExpression(JetConstantExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitSimpleNameExpression(JetSimpleNameExpression expression, D data) {
        return visitReferenceExpression(expression, data);
    }

    public R visitReferenceExpression(JetReferenceExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitPrefixExpression(JetPrefixExpression expression, D data) {
        return visitUnaryExpression(expression, data);
    }

    public R visitPostfixExpression(JetPostfixExpression expression, D data) {
        return visitUnaryExpression(expression, data);
    }

    public R visitUnaryExpression(JetUnaryExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitBinaryExpression(JetBinaryExpression expression, D data) {
        return visitExpression(expression, data);
    }

//    public R visitNewExpression(JetNewExpression expression, D data) {
//        return visitExpression(expression, data);
//    }
//
    public R visitReturnExpression(JetReturnExpression expression, D data) {
        return visitLabelQualifiedExpression(expression, data);
    }

    public R visitLabelQualifiedExpression(JetLabelQualifiedExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitThrowExpression(JetThrowExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitBreakExpression(JetBreakExpression expression, D data) {
        return visitLabelQualifiedExpression(expression, data);
    }

    public R visitContinueExpression(JetContinueExpression expression, D data) {
        return visitLabelQualifiedExpression(expression, data);
    }

    public R visitIfExpression(JetIfExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitWhenExpression(JetWhenExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitTryExpression(JetTryExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitForExpression(JetForExpression expression, D data) {
        return visitLoopExpression(expression, data);
    }

    public R visitWhileExpression(JetWhileExpression expression, D data) {
        return visitLoopExpression(expression, data);
    }

    public R visitDoWhileExpression(JetDoWhileExpression expression, D data) {
        return visitLoopExpression(expression, data);
    }

    public R visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitAnnotatedExpression(JetAnnotatedExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitCallExpression(JetCallExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitArrayAccessExpression(JetArrayAccessExpression expression, D data) {
        return visitReferenceExpression(expression, data);
    }

    public R visitQualifiedExpression(JetQualifiedExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitCallableReferenceExpression(JetCallableReferenceExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitDotQualifiedExpression(JetDotQualifiedExpression expression, D data) {
        return visitQualifiedExpression(expression, data);
    }

    public R visitSafeQualifiedExpression(JetSafeQualifiedExpression expression, D data) {
        return visitQualifiedExpression(expression, data);
    }

    public R visitObjectLiteralExpression(JetObjectLiteralExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitRootNamespaceExpression(JetRootNamespaceExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitBlockExpression(JetBlockExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitIdeTemplate(JetIdeTemplate expression, D data) {
        return null;
    }

    public R visitCatchSection(JetCatchClause catchClause, D data) {
        return visitJetElement(catchClause, data);
    }

    public R visitFinallySection(JetFinallySection finallySection, D data) {
        return visitJetElement(finallySection, data);
    }

    public R visitTypeArgumentList(JetTypeArgumentList typeArgumentList, D data) {
        return visitJetElement(typeArgumentList, data);
    }

    public R visitThisExpression(JetThisExpression expression, D data) {
        return visitLabelQualifiedExpression(expression, data);
    }

    public R visitSuperExpression(JetSuperExpression expression, D data) {
        return visitLabelQualifiedExpression(expression, data);
    }

    public R visitParenthesizedExpression(JetParenthesizedExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitInitializerList(JetInitializerList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitAnonymousInitializer(JetClassInitializer initializer, D data) {
        return visitDeclaration(initializer, data);
    }

    public R visitPropertyAccessor(JetPropertyAccessor accessor, D data) {
        return visitDeclaration(accessor, data);
    }

    public R visitTypeConstraintList(JetTypeConstraintList list, D data) {
        return visitJetElement(list, data);
    }

    public R visitTypeConstraint(JetTypeConstraint constraint, D data) {
        return visitJetElement(constraint, data);
    }

    private R visitTypeElement(JetTypeElement type, D data) {
        return visitJetElement(type, data);
    }

    public R visitUserType(JetUserType type, D data) {
        return visitTypeElement(type, data);
    }

    public R visitFunctionType(JetFunctionType type, D data) {
        return visitTypeElement(type, data);
    }

    public R visitSelfType(JetSelfType type, D data) {
        return visitTypeElement(type, data);
    }

    public R visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitStringTemplateExpression(JetStringTemplateExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitNamedDeclaration(JetNamedDeclaration declaration, D data) {
        return visitDeclaration(declaration, data);
    }

    public R visitNullableType(JetNullableType nullableType, D data) {
        return visitTypeElement(nullableType, data);
    }

    public R visitTypeProjection(JetTypeProjection typeProjection, D data) {
        return visitJetElement(typeProjection, data);
    }

    public R visitWhenEntry(JetWhenEntry jetWhenEntry, D data) {
        return visitJetElement(jetWhenEntry, data);
    }

    public R visitIsExpression(JetIsExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitWhenConditionIsPattern(JetWhenConditionIsPattern condition, D data) {
        return visitJetElement(condition, data);
    }

    public R visitWhenConditionInRange(JetWhenConditionInRange condition, D data) {
        return visitJetElement(condition, data);
    }
    
    public R visitWhenConditionExpression(JetWhenConditionWithExpression condition, D data) {
        return visitJetElement(condition, data);
    }

    public R visitObjectDeclaration(JetObjectDeclaration declaration, D data) {
        return visitNamedDeclaration(declaration, data);
    }
    
    public R visitObjectDeclarationName(JetObjectDeclarationName declarationName, D data) {
        return visitNamedDeclaration(declarationName, data);
    }

    public R visitStringTemplateEntry(JetStringTemplateEntry entry, D data) {
        return visitJetElement(entry, data);
    }

    public R visitStringTemplateEntryWithExpression(JetStringTemplateEntryWithExpression entry, D data) {
        return visitStringTemplateEntry(entry, data);
    }

    public R visitBlockStringTemplateEntry(JetBlockStringTemplateEntry entry, D data) {
        return visitStringTemplateEntryWithExpression(entry, data);
    }

    public R visitSimpleNameStringTemplateEntry(JetSimpleNameStringTemplateEntry entry, D data) {
        return visitStringTemplateEntryWithExpression(entry, data);
    }

    public R visitLiteralStringTemplateEntry(JetLiteralStringTemplateEntry entry, D data) {
        return visitStringTemplateEntry(entry, data);
    }

    public R visitEscapeStringTemplateEntry(JetEscapeStringTemplateEntry entry, D data) {
        return visitStringTemplateEntry(entry, data);
    }

}
