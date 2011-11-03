package org.jetbrains.jet.compiler;

import jet.modules.IModuleBuilder;
import jet.modules.IModuleSetBuilder;
import junit.framework.TestCase;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;

/**
 * @author yole
 */
public class CompileEnvironmentTest extends TestCase {
    private CompileEnvironment environment;

    protected void setUp() throws Exception {
        super.setUp();
        environment = new CompileEnvironment();
    }

    @Override
    protected void tearDown() throws Exception {
        environment.dispose();
        super.tearDown();
    }

    public void testSmoke() {
        final File activeRtJar = CompileEnvironment.findActiveRtJar(true);
        environment.setJavaRuntime(activeRtJar);
        environment.initializeKotlinRuntime();
        final String testDataDir = JetParsingTest.getTestDataDir() + "/compiler/smoke/";
        final IModuleSetBuilder setBuilder = environment.loadModuleScript(testDataDir + "Smoke.kts");
        assertEquals(1, setBuilder.getModules().size());
        final IModuleBuilder moduleBuilder = setBuilder.getModules().get(0);
        final ClassFileFactory factory = environment.compileModule(moduleBuilder, testDataDir);
        assertNotNull(factory);
        assertNotNull(factory.asBytes("Smoke/namespace.class"));
    }
}
