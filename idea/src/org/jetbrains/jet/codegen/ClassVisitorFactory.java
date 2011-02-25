package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.psi.JetNamespace;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author max
 */
public class ClassVisitorFactory {
    public ClassVisitor visitorForClassIn(JetNamespace namespace) {
        return new TraceClassVisitor(new PrintWriter(new StringWriter()));
    }
}
