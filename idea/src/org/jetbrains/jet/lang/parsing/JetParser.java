/*
 * @author max
 */
package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class JetParser implements PsiParser {
    @NotNull
    public ASTNode parse(IElementType iElementType, PsiBuilder psiBuilder) {
        JetParsing.createForTopLevel(new SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder)).parseFile();
        return psiBuilder.getTreeBuilt();
    }
}
