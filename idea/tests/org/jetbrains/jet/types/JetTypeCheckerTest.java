package org.jetbrains.jet.types;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.TypeResolver;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.util.List;

/**
 * @author abreslav
 */
public class JetTypeCheckerTest extends LightDaemonAnalyzerTestCase {

    private static final JetScope BASIC_SCOPE = new JetScope.JetScopeImpl() {
        @Override
        public ClassDescriptor getClass(String name) {
            if ("Int".equals(name)) {
                return JetStandardClasses.getInt();
            } else if ("Boolean".equals(name)) {
                return JetStandardClasses.getBoolean();
            } else if ("Byte".equals(name)) {
                return JetStandardClasses.getByte();
            } else if ("Char".equals(name)) {
                return JetStandardClasses.getChar();
            } else if ("Short".equals(name)) {
                return JetStandardClasses.getShort();
            } else if ("Long".equals(name)) {
                return JetStandardClasses.getLong();
            } else if ("Float".equals(name)) {
                return JetStandardClasses.getFloat();
            } else if ("Double".equals(name)) {
                return JetStandardClasses.getDouble();
            }
            fail("Type not found: " + name);
            throw new IllegalStateException();
        }
    };

    @Override
    protected String getTestDataPath() {
        return getHomeDirectory() + "/idea/testData";
    }

    private static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class")).getParentFile().getParentFile().getParent();
    }

    public void testConstants() throws Exception {
        assertType("1", JetStandardTypes.getInt());
        assertType("0x1", JetStandardTypes.getInt());
        assertType("0X1", JetStandardTypes.getInt());
        assertType("0b1", JetStandardTypes.getInt());
        assertType("0B1", JetStandardTypes.getInt());

        assertType("1l", JetStandardTypes.getLong());
        assertType("1L", JetStandardTypes.getLong());

        assertType("1.0", JetStandardTypes.getDouble());
        assertType("1.0d", JetStandardTypes.getDouble());
        assertType("1.0D", JetStandardTypes.getDouble());
        assertType("0x1.fffffffffffffp1023", JetStandardTypes.getDouble());

        assertType("1.0f", JetStandardTypes.getFloat());
        assertType("1.0F", JetStandardTypes.getFloat());
        assertType("0x1.fffffffffffffp1023f", JetStandardTypes.getFloat());

        assertType("true", JetStandardTypes.getBoolean());
        assertType("false", JetStandardTypes.getBoolean());

        assertType("'d'", JetStandardTypes.getChar());

        assertType("\"d\"", JetStandardTypes.getString());
        assertType("\"\"\"d\"\"\"", JetStandardTypes.getString());

        assertType("()", JetStandardTypes.getUnit());
    }

    public void testSubtyping() throws Exception {
        assertSubtype("Boolean", "Boolean");
        assertSubtype("Byte", "Byte");
        assertSubtype("Char", "Char");
        assertSubtype("Short", "Short");
        assertSubtype("Int", "Int");
        assertSubtype("Long", "Long");
        assertSubtype("Float", "Float");
        assertSubtype("Double", "Double");
        assertSubtype("Unit", "Unit");

        assertNotSubtype("Boolean", "Byte");
        assertNotSubtype("Byte", "Short");
        assertNotSubtype("Char", "Int");
        assertNotSubtype("Short", "Int");
        assertNotSubtype("Int", "Long");
        assertNotSubtype("Long", "Double");
        assertNotSubtype("Float", "Double");
        assertNotSubtype("Double", "Int");
        assertNotSubtype("Unit", "Unit");

        assertSubtype("(Boolean)", "(Boolean)");
        assertSubtype("(Byte)",    "(Byte)");
        assertSubtype("(Char)",    "(Char)");
        assertSubtype("(Short)",   "(Short)");
        assertSubtype("(Int)",     "(Int)");
        assertSubtype("(Long)",    "(Long)");
        assertSubtype("(Float)",   "(Float)");
        assertSubtype("(Double)",  "(Double)");
        assertSubtype("(Unit)",    "(Unit)");

    }

    private void assertSubtype(String type1, String type2) {
        assertSubtypingRelation(type1, type2, true);
    }

    private void assertNotSubtype(String type1, String type2) {
        assertSubtypingRelation(type1, type2, false);
    }

    private void assertSubtypingRelation(String type1, String type2, boolean expected) {
        Type typeNode1 = toType(JetChangeUtil.createType(getProject(), type1));
        Type typeNode2 = toType(JetChangeUtil.createType(getProject(), type2));
        boolean result = new JetTypeChecker().isSubtypeOf(
                typeNode1,
                typeNode2);
        assertTrue(typeNode1 + " is not a subtype of " + typeNode2, result == expected);
    }

    private Type toType(JetTypeReference typeNode) {
        List<JetAttribute> attributes = typeNode.getAttributes();
        JetTypeElement typeElement = typeNode.getTypeElement();
        List<JetTypeReference> typeArguments = typeNode.getTypeArguments();

        final Type[] result = new Type[1];
        typeElement.accept(new JetVisitor() {
            @Override
            public void visitUserType(JetUserType type) {
                result[0] = new ClassType(TypeResolver.INSTANCE.resolveClass(BASIC_SCOPE, type));
            }

            @Override
            public void visitJetElement(JetElement elem) {
                throw new IllegalArgumentException();
            }
        });

        return result[0];
    }

    public void testImplicitConversions() throws Exception {
        assertConvertibleTo("1", JetStandardTypes.getByte());
    }

    private static void assertConvertibleTo(String expression, Type type) {
        JetExpression jetExpression = JetChangeUtil.createExpression(getProject(), expression);
        assertTrue(
                expression + " must be convertible to " + type,
                new JetTypeChecker().isConvertibleTo(jetExpression, type));
    }

    private static void assertNotConvertibleTo(String expression, Type type) {
        JetExpression jetExpression = JetChangeUtil.createExpression(getProject(), expression);
        assertFalse(
                expression + " must not be convertible to " + type,
                new JetTypeChecker().isConvertibleTo(jetExpression, type));
    }

    private static void assertType(String expression, Type expectedType) {
        Project project = getProject();
        JetExpression jetExpression = JetChangeUtil.createExpression(project, expression);
        Type type = new JetTypeChecker().getType(jetExpression);
        assertEquals(type, expectedType);
    }
}
