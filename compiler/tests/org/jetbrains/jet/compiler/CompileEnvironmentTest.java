package org.jetbrains.jet.compiler;

import jet.modules.IModuleBuilder;
import jet.modules.IModuleSetBuilder;
import junit.framework.TestCase;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.parsing.JetParsingTest;

/**
 * @author yole
 */
public class CompileEnvironmentTest extends TestCase {
    public void testSmoke() {
        CompileEnvironment environment = new CompileEnvironment();
        environment.setJavaRuntime(CompileEnvironment.findActiveRtJar());
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
