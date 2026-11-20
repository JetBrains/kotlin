// WITH_FIR_TEST_COMPILER_PLUGIN
// FILE: main.kt
fun test() {
    val foo = B().fo<caret>o
}

// FILE: B.java
@org.jetbrains.kotlin.plugin.sandbox.JavaClassWithGeneratedGetter
public class B {
}
