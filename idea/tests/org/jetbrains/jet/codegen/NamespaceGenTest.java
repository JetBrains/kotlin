package org.jetbrains.jet.codegen;

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.parsing.JetParsingTest;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author yole
 */
public class NamespaceGenTest extends LightCodeInsightFixtureTestCase {
    public void testPSVM() {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/PSVM.jet");
        JetFile jetFile = (JetFile) myFixture.getFile();
        JetNamespace namespace = jetFile.getRootNamespace();
        NamespaceCodegen codegen = new NamespaceCodegen();
        StringWriter writer = new StringWriter();
        codegen.generate(namespace, new TraceClassVisitor(new PrintWriter(writer)));
    }
}
