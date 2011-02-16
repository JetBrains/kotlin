package org.jetbrains.jet.lang.annotations;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

import java.util.List;

/**
 * @author abreslav
 */
public class JetPsiChecker implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof JetFile) {
//            JetProperty property = (JetProperty) element;
//            holder.createErrorAnnotation(property.getNameIdentifier(), "Specify either type or value");
            JetFile file = (JetFile) element;
            String path = file.getProject().getBaseDir().getPath();
            println("Path: " + path);
            JetNamespace rootNamespace = file.getRootNamespace();
            List<JetDeclaration> declarations = rootNamespace.getDeclarations();

            for (JetDeclaration declaration : declarations) {
                declaration.accept(new JetVisitor() {
                    @Override
                    public void visitClass(JetClass klass) {
                        print("class ");
                        print(klass.getName());
                        println("{");
                        for (JetDeclaration decl : klass.getDeclarations()) {
                            decl.accept(this);
                        }
                        print("}");
                    }

                    @Override
                    public void visitProperty(JetProperty property) {
                        JetTypeReference propertyTypeRef = property.getPropertyTypeRef();
                        String name = property.getName();
                    }

                    @Override
                    public void visitFunction(JetFunction function) {
                        super.visitFunction(function);    //To change body of overridden methods use File | Settings | File Templates.
                    }
                });
            }
        }
    }

    private void print(Object o) {
//        System.out.print(o);
    }

    private void println(Object o) {
//        System.out.println(o);
    }
}
