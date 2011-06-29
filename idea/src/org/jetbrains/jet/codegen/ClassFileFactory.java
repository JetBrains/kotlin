package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * @author max
 */
public class ClassFileFactory {
    private final Project project;
    private final boolean isText;
    private final Map<String, NamespaceCodegen> ns2codegen = new HashMap<String, NamespaceCodegen>();
    private final Map<String, ClassVisitor> generators = new LinkedHashMap<String, ClassVisitor>();
    private boolean isDone = false;
    public final GenerationState state;

    public ClassFileFactory(Project project, boolean text, GenerationState state) {
        this.project = project;
        isText = text;
        this.state = state;
    }

    ClassVisitor newVisitor(String filePath) {
        ClassVisitor visitor;
        if (isText) {
            visitor = new TraceClassVisitor(new PrintWriter(new StringWriter()));
        }
        else {
            visitor = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        }

        generators.put(filePath, visitor);
        return visitor;
    }

    ClassVisitor forAnonymousSubclass(String className) {
        return newVisitor(className + ".class");
    }

    NamespaceCodegen forNamespace(JetNamespace namespace) {
        assert !isDone : "Already done!";
        String fqName = namespace.getFQName();
        NamespaceCodegen codegen = ns2codegen.get(fqName);
        if (codegen == null) {
            codegen = new NamespaceCodegen(project, newVisitor(NamespaceCodegen.getJVMClassName(fqName) + ".class"), fqName, state);
            ns2codegen.put(fqName, codegen);
        }

        return codegen;
    }

    private void done() {
        if (!isDone) {
            isDone = true;
            for (NamespaceCodegen codegen : ns2codegen.values()) {
                codegen.done();
            }
        }
    }

    public String asText(String file) {
        assert isText : "Need to create with text=true";

        done();

        TraceClassVisitor visitor = (TraceClassVisitor) generators.get(file);

        StringWriter writer = new StringWriter();
        visitor.print(new PrintWriter(writer));

        return writer.toString();
    }

    public byte[] asBytes(String file) {
        assert !isText : "This is debug stuff, only produces texts.";

        done();

        ClassWriter visitor = (ClassWriter) generators.get(file);
        return visitor.toByteArray();
    }

    public List<String> files() {
        done();
        return new ArrayList<String>(generators.keySet());
    }
}
