package org.jetbrains.jet.lang.psi;

import com.intellij.psi.PsiElementVisitor;

/**
 * @author max
 */
public class JetVisitor extends PsiElementVisitor {
    public void visitJetElement(JetElement elem) {
        visitElement(elem);
    }

    public void visitDeclaration(JetDeclaration dcl) {
        visitJetElement(dcl);
    }

    public void visitNamespace(JetNamespace namespace) {
        visitDeclaration(namespace);
    }

    public void visitClass(JetClass klass) {
        visitNamedDeclaration(klass);
    }

    public void visitClassObject(JetClassObject classObject) {
        visitDeclaration(classObject);
    }

    public void visitConstructor(JetConstructor constructor) {
        visitDeclaration(constructor);
    }

    public void visitExtension(JetExtension extension) {
        visitNamedDeclaration(extension);
    }

    public void visitFunction(JetFunction function) {
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

    public void visitImportDirective(JetImportDirective importDirective) {
        visitJetElement(importDirective);
    }

    public void visitClassBody(JetClassBody classBody) {
        visitJetElement(classBody);
    }

    public void visitNamespaceBody(JetNamespaceBody body) {
        visitJetElement(body);
    }

    public void visitModifierList(JetModifierList list) {
        visitJetElement(list);
    }

    public void visitAttributeAnnotation(JetAttributeAnnotation annotation) {
        visitJetElement(annotation);
    }

    public void visitAttribute(JetAttribute attribute) {
        visitJetElement(attribute);
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

    public void visitTypeReference(JetTypeReference typeReference) {
        visitJetElement(typeReference);
    }

    public void visitArgumentList(JetArgumentList list) {
        visitJetElement(list);
    }

    public void visitArgument(JetArgument argument) {
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
        visitExpression(expression);
    }

    public void visitTupleExpression(JetTupleExpression expression) {
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

    public void visitTypeofExpression(JetTypeofExpression expression) {
        visitExpression(expression);
    }

    public void visitBinaryExpression(JetBinaryExpression expression) {
        visitExpression(expression);
    }

    public void visitNewExpression(JetNewExpression expression) {
        visitExpression(expression);
    }

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
        visitExpression(expression);
    }

    public void visitArrayAccessExpression(JetArrayAccessExpression expression) {
        visitExpression(expression);
    }

    public void visitQualifiedExpression(JetQualifiedExpression expression) {
        visitExpression(expression);
    }

    public void visitHashQualifiedExpression(JetHashQualifiedExpression expression) {
        visitQualifiedExpression(expression);
    }

    public void visitDotQualifiedExpression(JetDotQualifiedExpression expression) {
        visitQualifiedExpression(expression);
    }

    public void visitPredicateExpression(JetPredicateExpression expression) {
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

    public void visitTupleType(JetTupleType type) {
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

    public void visitWhenConditionWithExpression(JetWhenConditionWithExpression condition) {
        visitJetElement(condition);
    }

    public void visitWhenConditionCall(JetWhenConditionCall condition) {
        visitJetElement(condition);
    }

    public void visitWhenConditionIsPattern(JetWhenConditionIsPattern condition) {
        visitJetElement(condition);
    }

    public void visitWhenConditionInRange(JetWhenConditionInRange condition) {
        visitJetElement(condition);
    }

    public void visitTypePattern(JetTypePattern typePattern) {
        visitPattern(typePattern);
    }

    public void visitPattern(JetPattern pattern) {
        visitJetElement(pattern);
    }

    public void visitWildcardPattern(JetWildcardPattern pattern) {
        visitPattern(pattern);
    }
}
