package org.jetbrains.jet.lang.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.LocalTimeCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetFileType;

import java.util.List;

/**
 * @author max
 */
public class JetChangeUtil {
    @NotNull
    public static JetFile createFile(Project project, String text) {
        return (JetFile) PsiFileFactory.getInstance(project).createFileFromText("dummy.jet", JetFileType.INSTANCE, text, LocalTimeCounter.currentTime(), true);
    }

    public static JetProperty createProperty(Project project, String name, String type) {
        JetFile file = createFile(project, "val " + name + (type != null ? ":" + type : ""));
        JetNamespace rootNamespace = file.getRootNamespace();
        List<JetDeclaration> dcls = rootNamespace.getDeclarations();
        assert dcls.size() == 1;
        return (JetProperty) dcls.get(0);
    }

    public static PsiElement createNameIdentifier(Project project, String name) {
        return createProperty(project, name, null).getNameIdentifier();
    }
}
