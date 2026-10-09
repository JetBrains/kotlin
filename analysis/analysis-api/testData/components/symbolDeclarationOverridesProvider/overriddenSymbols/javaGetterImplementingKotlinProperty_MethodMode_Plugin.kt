// WITH_FIR_TEST_COMPILER_PLUGIN
// callable: /B.getFoo

// FILE: main.kt
interface A { val foo: Int }

// FILE: B.java
@@org.jetbrains.kotlin.plugin.sandbox.JavaClassWithGeneratedGetter
public class B implements A {
}
