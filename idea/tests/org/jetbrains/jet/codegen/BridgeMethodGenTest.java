package org.jetbrains.jet.codegen;

import java.util.List;

public class BridgeMethodGenTest extends CodegenTestCase {
    public void testBridgeMethod () throws Exception {
        blackBoxFile("bridge.jet");
    }
}
