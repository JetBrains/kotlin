package org.jetbrains.jet.codegen;

public class BridgeMethodGenTest extends CodegenTestCase {
    public void testBridgeMethod () throws Exception {
        blackBoxFile("bridge.jet");
    }
}
