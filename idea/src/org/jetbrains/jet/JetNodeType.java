/*
 * @author max
 */
package org.jetbrains.jet;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetLanguage;

public class JetNodeType extends IElementType {
    public JetNodeType(@NotNull @NonNls String debugName) {
        super(debugName, JetLanguage.INSTANCE);
    }
}
