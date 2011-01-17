package org.jetbrains.jet.parsing;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.psi.JetChangeUtil;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.types.JetStandardTypes;
import org.jetbrains.jet.lang.types.Type;
import org.jetbrains.jet.lang.types.JetTypeChecker;

import java.io.File;

/**
 * @author abreslav
 */
public class JetTypeCheckerTest extends LightDaemonAnalyzerTestCase {

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

    public void testImplicitConversions() throws Exception {
        assertConvertibleTo("1", JetStandardTypes.getByte());
    }

    private void assertConvertibleTo(String expression, Type type) {
        JetExpression jetExpression = JetChangeUtil.createExpression(getProject(), expression);
        assertTrue(
                expression + " must be convertible to " + type,
                new JetTypeChecker().isConvertibleTo(jetExpression, type));
    }

    private void assertNotConvertibleTo(String expression, Type type) {
        JetExpression jetExpression = JetChangeUtil.createExpression(getProject(), expression);
        assertFalse(
                expression + " must not be convertible to " + type,
                new JetTypeChecker().isConvertibleTo(jetExpression, type));
    }

    private void assertType(String expression, Type expectedType) {
        Project project = getProject();
        JetExpression jetExpression = JetChangeUtil.createExpression(project, expression);
        Type type = new JetTypeChecker().getType(jetExpression);
        assertEquals(type, expectedType);
    }
}
