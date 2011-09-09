/*
 * @author max
 */
package org.jetbrains.jet.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.FlexLexer;

import java.io.Reader;

public class JetLexer extends FlexAdapter {
    public JetLexer() {
        super(new _JetLexer((Reader) null));
    }
}
