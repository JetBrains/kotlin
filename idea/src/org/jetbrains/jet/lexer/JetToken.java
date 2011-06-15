/*
 * @author max
 */
package org.jetbrains.jet.lexer;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLanguage;

public class JetToken extends IElementType {
    public JetToken(@NotNull @NonNls String debugName) {
        super(debugName, JetLanguage.INSTANCE);
    }
}
