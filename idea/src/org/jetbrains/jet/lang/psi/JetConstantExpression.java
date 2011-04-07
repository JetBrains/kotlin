package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetConstantExpression extends JetExpression {
    public JetConstantExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitConstantExpression(this);
    }

    public Object getValue() {
        IElementType elementType = getNode().getElementType();
        String nodeText = getNode().getText();

        if (elementType == JetNodeTypes.INTEGER_CONSTANT) {
            try {
                if (nodeText.startsWith("0x") || nodeText.startsWith("0X")) {
                    String hexString = nodeText.substring(2);
                    int length = hexString.length();
                    if (length > 8) {
                        return Long.parseLong(hexString);
                    }
                    return getRangedValue(Long.parseLong(hexString, 16));
                }
                if (nodeText.startsWith("0b") || nodeText.startsWith("0B")) {
                    return getRangedValue(Long.parseLong(nodeText.substring(2), 2));
                }
                return getRangedValue(Long.parseLong(nodeText));
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
        else if (elementType == JetNodeTypes.LONG_CONSTANT) {
            if (nodeText.endsWith("l") || nodeText.endsWith("L")) {
                return Long.parseLong(nodeText.substring(0, nodeText.length() - 1));
            }
            return Long.parseLong(nodeText);
        }
        else if (elementType == JetNodeTypes.FLOAT_CONSTANT) {
            assert nodeText.length() > 0;
            char lastChar = nodeText.charAt(nodeText.length() - 1);
            if (lastChar == 'f' || lastChar == 'F') {
                return Float.parseFloat(nodeText.substring(0, nodeText.length() - 1));
            }
            else if (lastChar == 'd' || lastChar == 'D') {
                return Double.parseDouble(nodeText.substring(0, nodeText.length() - 1));
            }
            else {
                return Double.parseDouble(nodeText);
            }
        }
        else if (elementType == JetNodeTypes.BOOLEAN_CONSTANT) {
            return Boolean.parseBoolean(nodeText);
        }
        else if (elementType == JetNodeTypes.CHARACTER_CONSTANT) {
            return nodeText.charAt(1);
        }
        else if (elementType == JetNodeTypes.STRING_CONSTANT) {
            int tail = nodeText.length();
            if (nodeText.endsWith("\"")) tail--;
            return nodeText.substring(1, tail);
        }
        else if (elementType == JetNodeTypes.NULL) {
            return null;
        }
        else {
            throw new IllegalArgumentException("Unsupported constant: " + this);
        }
    }

    private Object getRangedValue(long longValue) {
        if (Byte.MIN_VALUE <= longValue && longValue <= Byte.MAX_VALUE) {
                    return (byte) longValue;
                }
        if (Short.MIN_VALUE <= longValue && longValue <= Short.MAX_VALUE) {
                    return (short) longValue;
                }
        if (Integer.MIN_VALUE <= longValue && longValue <= Integer.MAX_VALUE) {
                    return (int) longValue;
                }
        return longValue;
    }
}
