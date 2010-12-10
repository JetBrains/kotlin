/*
 * @author max
 */
package org.jetbrains.jet;

import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.jet.lang.JetLanguage;

public interface JetNodeTypes {
    IFileElementType JET_FILE_NODE = new IFileElementType(JetLanguage.INSTANCE);

}
