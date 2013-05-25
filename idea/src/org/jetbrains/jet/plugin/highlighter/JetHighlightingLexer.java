package org.jetbrains.jet.plugin.highlighter;

import com.intellij.lexer.LayeredLexer;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.jet.kdoc.lexer.KDocLexer;
import org.jetbrains.jet.lexer.JetLexer;
import org.jetbrains.jet.lexer.JetTokens;

public class JetHighlightingLexer extends LayeredLexer {
    public JetHighlightingLexer() {
        super(new JetLexer());

        registerSelfStoppingLayer(new KDocLexer(), new IElementType[]{JetTokens.DOC_COMMENT}, IElementType.EMPTY_ARRAY);
    }
}
