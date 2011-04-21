package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.ClassDescriptor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author max
 */
public class Codegens {
    private final Project project;
    private final boolean isText;
    private final Map<String, NamespaceCodegen> ns2codegen = new HashMap<String, NamespaceCodegen>();
    private final Map<String, ClassVisitor> generators = new HashMap<String, ClassVisitor>();
    private boolean isDone = false;

    public Codegens(Project project, boolean text) {
        this.project = project;
        isText = text;
    }

    public ClassVisitor newVisitor(String filePath) {
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

    public ClassVisitor forClassInterface(ClassDescriptor aClass) {
        return newVisitor(JetTypeMapper.jvmNameForInterface(aClass) + ".class");
    }

    public ClassCodegen forClass(BindingContext bindingContext) {
        return new ClassCodegen(project, this, bindingContext);
    }

    public ClassVisitor forClassImplementation(ClassDescriptor aClass) {
        return newVisitor(JetTypeMapper.jvmNameForImplementation(aClass) + ".class");
    }

    public ClassVisitor forClassDelegatingImplementation(ClassDescriptor aClass) {
        return newVisitor(JetTypeMapper.jvmNameForDelegatingImplementation(aClass)  + ".class");
    }

    public NamespaceCodegen forNamespace(JetNamespace namespace) {
        assert !isDone : "Already done!";
        String fqName = namespace.getFQName();
        NamespaceCodegen codegen = ns2codegen.get(fqName);
        if (codegen == null) {
            codegen = new NamespaceCodegen(project, newVisitor(NamespaceCodegen.getJVMClassName(fqName) + ".class"), fqName, this);
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
