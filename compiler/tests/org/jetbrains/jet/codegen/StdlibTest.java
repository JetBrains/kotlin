package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.psi.JetNamespace;

import java.lang.reflect.Method;

/**
 * @author alex.tkachman
 */
public class StdlibTest extends CodegenTestCase {
    public void testLib () throws ClassNotFoundException {
        createEnvironmentWithFullJdk();
        loadFile("../../../stdlib/ktSrc/StandardLibrary.kt");
        ClassFileFactory codegens = generateClassesInFile();
        GeneratedClassLoader loader = new GeneratedClassLoader(codegens);

        final JetNamespace namespace = myFile.getRootNamespace();
        String fqName = NamespaceCodegen.getJVMClassName(CodegenUtil.getFQName(namespace)).replace("/", ".");
        Class<?> namespaceClass = loader.loadClass(fqName);
    }

    public void testInputStreamIterator () {
        blackBoxFile("inputStreamIterator.jet");
    }
}
