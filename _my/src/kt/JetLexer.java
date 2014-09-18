package kt;

import com.intellij.lexer.FlexAdapter;

/**
 * Created by user on 8/13/14.
 */
public class JetLexer extends FlexAdapter {
    public JetLexer() {
        super(new _JetLexer());
    }
}
