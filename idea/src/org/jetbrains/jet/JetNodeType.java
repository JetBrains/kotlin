/*
 * @author max
 */
package org.jetbrains.jet;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.lang.psi.JetElement;

import java.lang.reflect.Constructor;

public class JetNodeType extends IElementType {
    private Constructor<? extends JetElement> myPsiFactory;

    public JetNodeType(@NotNull @NonNls String debugName) {
        this(debugName, null);
    }

    public JetNodeType(@NotNull @NonNls String debugName, Class<? extends JetElement> psiClass) {
        super(debugName, JetLanguage.INSTANCE);
        try {
            myPsiFactory = psiClass != null ? psiClass.getConstructor(ASTNode.class) : null;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Must have a constructor with ASTNode");
        }
    }

    public JetElement createPsi(ASTNode node) {
        assert node.getElementType() == this;

        try {
            if (myPsiFactory == null) {
                return new JetElement(node);
            }
            return myPsiFactory.newInstance(node);
        } catch (Exception e) {
            throw new RuntimeException("Error creating psi element for node", e);
        }
    }
}
