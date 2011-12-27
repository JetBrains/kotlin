package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;

import java.util.*;

/**
 * @author max
 */
public class ClassFileFactory {
    private final ClassBuilderFactory builderFactory;
    private final Map<String, NamespaceCodegen> ns2codegen = new HashMap<String, NamespaceCodegen>();
    private final Map<String, ClassBuilder> generators = new LinkedHashMap<String, ClassBuilder>();
    private boolean isDone = false;
    public final GenerationState state;

    public ClassFileFactory(ClassBuilderFactory builderFactory, GenerationState state) {
        this.builderFactory = builderFactory;
        this.state = state;
    }

    ClassBuilder newVisitor(String filePath) {
        final ClassBuilder answer = builderFactory.newClassBuilder();
        generators.put(filePath, answer);
        return answer;
    }

    ClassBuilder forAnonymousSubclass(String className) {
        return newVisitor(className + ".class");
    }

    NamespaceCodegen forNamespace(JetFile file) {
        assert !isDone : "Already done!";
        String fqName = JetPsiUtil.getFQName(file);
        NamespaceCodegen codegen = ns2codegen.get(fqName);
        if (codegen == null) {
            final ClassBuilder builder = newVisitor(NamespaceCodegen.getJVMClassName(fqName) + ".class");
            codegen = new NamespaceCodegen(builder, fqName, state, file.getContainingFile());
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
        done();
        return builderFactory.asText(generators.get(file));
    }

    public byte[] asBytes(String file) {
        done();
        return builderFactory.asBytes(generators.get(file));
    }

    public List<String> files() {
        done();
        return new ArrayList<String>(generators.keySet());
    }
}
