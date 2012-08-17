package org.jetbrains.jet.lang.psi;

import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.Name;

/**
 * @author Nikolay Krasko
 */
public interface JetNamedDeclaration extends JetDeclaration, PsiNameIdentifierOwner, JetStatementExpression, JetNamed {
    @NotNull
    Name getNameAsSafeName();
}
