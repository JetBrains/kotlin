package org.jetbrains.jet.plugin.findUsages;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.jet.lexer.JetLexer;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author yole
 */
public class JetWordsScanner extends DefaultWordsScanner {
    public JetWordsScanner() {
        super(new JetLexer(),
                TokenSet.create(JetTokens.IDENTIFIER),
                JetTokens.COMMENTS,
                JetTokens.STRINGS);
    }
}
