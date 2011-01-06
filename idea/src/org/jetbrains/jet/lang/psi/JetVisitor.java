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
        visitDeclaration(klass);
    }

    public void visitClassObject(JetClassObject classObject) {
        visitDeclaration(classObject);
    }

    public void visitConstructor(JetConstructor constructor) {
        visitDeclaration(constructor);
    }

    public void visitDecomposer(JetDecomposer decomposer) {
        visitDeclaration(decomposer);
    }

    public void visitExtension(JetExtension extension) {
        visitDeclaration(extension);
    }

    public void visitFunction(JetFunction fun) {
        visitDeclaration(fun);
    }

    public void visitProperty(JetProperty property) {
        visitDeclaration(property);
    }

    public void visitTypedef(JetTypedef typedef) {
        visitDeclaration(typedef);
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
        visitDeclaration(parameter);
    }

    public void visitEnumEntry(JetEnumEntry enumEntry) {
        visitClass(enumEntry);
    }

    public void visitParameterList(JetParameterList list) {
        visitJetElement(list);
    }

    public void visitParameter(JetParameter parameter) {
        visitDeclaration(parameter);
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

    public void visitConstantExpression(JetConstantExpression expression) {
        visitExpression(expression);
    }

    public void visitReferenceExpression(JetReferenceExpression expression) {
        visitExpression(expression);
    }

    public void visitTupleExpression(JetTupleExpression expression) {
        visitExpression(expression);
    }

    public void visitPrefixExpression(JetPrefixExpression expression) {
        visitExpression(expression);
    }

    public void visitPostfixExpression(JetPostfixExpression expression) {
        visitExpression(expression);
    }

    public void visitTypeofExpression(JetTypeofExpression expression) {
        visitExpression(expression);
    }
}
