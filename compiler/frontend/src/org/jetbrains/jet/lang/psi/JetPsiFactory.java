package org.jetbrains.jet.lang.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.LocalTimeCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.List;

/**
 * @author max
 */
public class JetPsiFactory {
    public static JetExpression createExpression(Project project, String text) {
        JetProperty property = createProperty(project, "val x = " + text);
        return property.getInitializer();
    }

    public static JetTypeReference createType(Project project, String type) {
        JetProperty property = createProperty(project, "val x : " + type);
        return property.getPropertyTypeRef();
    }

    public static PsiElement[] createColon(Project project) {
        JetProperty property = createProperty(project, "val x : Int");
        return new PsiElement[] { property.findElementAt(5), property.findElementAt(6), property.findElementAt(7) };
    }

    public static PsiElement createWhiteSpace(Project project) {
        return createWhiteSpace(project, " ");
    }

    public static PsiElement createWhiteSpace(Project project, String text) {
        JetProperty property = createProperty(project, "val" + text + "x");
        return property.findElementAt(3);
    }

//    public static PsiElement createEndOfLine(Project project) {
//        JetNamedFunction function = createFunction(project, "fun f { \n }");
//        return function.findElementAt(8);
//    }

    public static JetClass createClass(Project project, String text) {
        return createDeclaration(project, text, JetClass.class);
    }

    @NotNull
    public static JetFile createFile(Project project, String text) {
        return (JetFile) PsiFileFactory.getInstance(project).createFileFromText("dummy.jet", JetFileType.INSTANCE, text, LocalTimeCounter.currentTime(), true);
    }

    public static JetProperty createProperty(Project project, String name, String type, boolean isVar) {
        String text = (isVar ? "var " : "val ") + name + (type != null ? ":" + type : "");
        return createProperty(project, text);
    }

    private static JetProperty createProperty(Project project, String text) {
        return createDeclaration(project, text, JetProperty.class);
    }

    private static <T> T createDeclaration(Project project, String text, Class<T> clazz) {
        JetFile file = createFile(project, text);
        JetNamespace rootNamespace = file.getRootNamespace();
        List<JetDeclaration> dcls = rootNamespace.getDeclarations();
        assert dcls.size() == 1 : dcls.size();
        @SuppressWarnings("unchecked")
        T result = (T) dcls.get(0);
        return result;
    }

    public static PsiElement createNameIdentifier(Project project, String name) {
        return createProperty(project, name, null, false).getNameIdentifier();
    }

    public static JetNamedFunction createFunction(Project project, String funDecl) {
        return createDeclaration(project, funDecl, JetNamedFunction.class);
    }

    public static JetModifierList createModifier(Project project, JetKeywordToken modifier) {
        String text = modifier.getValue() + " val x";
        JetProperty property = createProperty(project, text);
        return property.getModifierList();
    }

    public static JetExpression createEmptyBody(Project project) {
        JetNamedFunction function = createFunction(project, "fun foo() {}");
        return function.getBodyExpression();
    }

    public static JetNamespace createNamespace(Project project, String text) {
        JetFile file = createFile(project, text);
        return file.getRootNamespace();
    }

    public static JetImportDirective createImportDirective(Project project, String classPath) {
        JetNamespace namespace = createNamespace(project, "import " + classPath);
        return namespace.getImportDirectives().iterator().next();
    }
}
