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
}
