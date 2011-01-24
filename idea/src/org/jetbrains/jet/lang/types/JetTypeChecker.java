package org.jetbrains.jet.lang.types;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author abreslav
 */
public class JetTypeChecker {
    public Type getType(JetExpression expression) {
        final Type[] result = new Type[1];
        expression.accept(new JetVisitor() {
            @Override
            public void visitConstantExpression(JetConstantExpression expression) {
                IElementType elementType = expression.getNode().getElementType();
                if (elementType == JetNodeTypes.INTEGER_CONSTANT) {
                    result[0] = JetStandardTypes.getInt();
                } else if (elementType == JetNodeTypes.LONG_CONSTANT) {
                    result[0] = JetStandardTypes.getLong();
                } else if (elementType == JetNodeTypes.FLOAT_CONSTANT) {
                    String text = expression.getText();
                    assert text.length() > 0;
                    char lastChar = text.charAt(text.length() - 1);
                    if (lastChar == 'f' || lastChar == 'F') {
                        result[0] = JetStandardTypes.getFloat();
                    } else {
                        result[0] = JetStandardTypes.getDouble();
                    }
                } else if (elementType == JetNodeTypes.BOOLEAN_CONSTANT) {
                    result[0] = JetStandardTypes.getBoolean();
                } else if (elementType == JetNodeTypes.CHARACTER_CONSTANT) {
                    result[0] = JetStandardTypes.getChar();
                } else if (elementType == JetNodeTypes.STRING_CONSTANT) {
                    result[0] = JetStandardTypes.getString();
                } else {
                    throw new IllegalArgumentException("Unsupported constant: " + expression);
                }
            }

            @Override
            public void visitTupleExpression(JetTupleExpression expression) {
                List<JetExpression> entries = expression.getEntries();
                List<Type> types = new ArrayList<Type>();
                for (JetExpression entry : entries) {
                    types.add(getType(entry));
                }
                result[0] = TupleType.getTupleType(types);
            }

            @Override
            public void visitJetElement(JetElement elem) {
                throw new IllegalArgumentException("Unsupported element: " + elem);
            }
        });
        return result[0];
    }

    public boolean isConvertibleTo(JetExpression expression, Type type) {
        return false;  //To change body of created methods use File | Settings | File Templates.
    }

    public boolean isSubtypeOf(Type subtype, Type supertype) {
//        subtype.getConstructor().getSupertypes()
        return false;
    }
}
