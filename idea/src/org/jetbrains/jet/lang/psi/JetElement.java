package org.jetbrains.jet.lang.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLanguage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author max
 */
public class JetElement extends ASTWrapperPsiElement {
    public JetElement(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Comes along with @Nullable to indicate null is only possible if parsing error present
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public static @interface IfNotParsed {}

    @NotNull
    @Override
    public Language getLanguage() {
        return JetLanguage.INSTANCE;
    }

    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }

    @Override
    public final void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JetVisitor) {
            accept((JetVisitor) visitor);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitJetElement(this);
    }
}
