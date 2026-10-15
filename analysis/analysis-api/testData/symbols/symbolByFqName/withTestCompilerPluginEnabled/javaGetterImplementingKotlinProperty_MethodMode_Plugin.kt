// WITH_FIR_TEST_COMPILER_PLUGIN

// FILE: main.kt

package test

interface A { val foo: Int }

// FILE: B.java

package test;

@org.jetbrains.kotlin.plugin.sandbox.JavaClassWithGeneratedGetter
public class B implements A {
}

// callable: test/B.getFoo
