/*
 * @author max
 */
package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

public class JetParser implements PsiParser {
    @NotNull
    public ASTNode parse(IElementType iElementType, PsiBuilder psiBuilder) {
        PsiBuilder.Marker mark = psiBuilder.mark();
        while (!psiBuilder.eof()) {
            psiBuilder.advanceLexer();
        }
        mark.done(JetNodeTypes.JET_FILE_NODE);

        return psiBuilder.getTreeBuilt();
    }
}
