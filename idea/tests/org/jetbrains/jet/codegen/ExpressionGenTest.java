package org.jetbrains.jet.codegen;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author max
 */
public class ExpressionGenTest extends LightDaemonAnalyzerTestCase {
    public void testIntegerZeroExpression() throws Exception {
        checkCode("0",
                "LDC 0\n" +
                "INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;");
    }

    public void testIf() throws Exception {
        checkCode("if (false) 15 else 20",
                "LDC false\n" +
                "INVOKESTATIC java/lang/Boolean.valueOf (Z)Ljava/lang/Boolean;\n" +
                "INVOKEVIRTUAL java/lang/Boolean.booleanValue ()Z\n" +
                "IFEQ L0\n" +
                "LDC 15\n" +
                "INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;\n" +
                "GOTO L1\n" +
                "L0\n" +
                "LDC 20\n" +
                "INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;\n" +
                "L1");

        checkCode("if (false) 15",
                "LDC false\n" +
                "INVOKESTATIC java/lang/Boolean.valueOf (Z)Ljava/lang/Boolean;\n" +
                "INVOKEVIRTUAL java/lang/Boolean.booleanValue ()Z\n" +
                "IFEQ L0\n" +
                "LDC 15\n" +
                "INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;\n" +
                "POP\n" +
                "L0");

        checkCode("if (false) else 20",
                "LDC false\n" +
                "INVOKESTATIC java/lang/Boolean.valueOf (Z)Ljava/lang/Boolean;\n" +
                "INVOKEVIRTUAL java/lang/Boolean.booleanValue ()Z\n" +
                "IFNE L0\n" +
                "LDC 20\n" +
                "INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;\n" +
                "POP\n" +
                "L0");
    }

    private void checkCode(String expr, String expectation) throws IOException {
        configureFromFileText("test.jet", "val x = " + expr);

        JetProperty p = PsiTreeUtil.getParentOfType(getFile().findElementAt(0), JetProperty.class);

        TraceMethodVisitor trace = new TraceMethodVisitor();
        p.getInitializer().accept(new ExpressionCodegen(trace, null, new FrameMap()));

        StringWriter out = new StringWriter();
        trace.print(new PrintWriter(out));
        assertEquals(trimLines(expectation), trimLines(out.getBuffer().toString()));
    }

    private static String trimLines(String s) {
        StringBuilder b = new StringBuilder();
        for (String line : s.split("\\n")) {
            b.append(line.trim()).append('\n');
        }
        return b.toString();
    }
}
