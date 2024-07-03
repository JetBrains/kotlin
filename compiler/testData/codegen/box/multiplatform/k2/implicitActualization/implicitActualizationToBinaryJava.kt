// IGNORE_BACKEND: WASM, JS_IR, JS_IR_ES6, NATIVE
// WITH_STDLIB
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

// open class CommonBase { // copy-paste
//     fun commonFakeOverride(): Int = 2
//     open fun commonOverride(): Int = null!! // 7
// }

@OptIn(ExperimentalMultiplatform::class)
@kotlin.jvm.ImplicitlyActualizedByJvmDeclaration
expect class Foo() {
    fun foo(): Int // 5
}

fun common(): Int = 5 // Foo().foo() //* Foo().commonFakeOverride() * Foo().commonOverride()

// MODULE: lib()()()
// FILE: Foo.java
public class Foo {
    public int foo() { return 5; }
    public void javaLib() {}
    // @Override public int commonOverride() { return 7; }
}

// // FILE: lib.kt
// open class CommonBase { // copy-paste
//     fun commonFakeOverride(): Int = 2
//     open fun commonOverride(): Int = null!! // 7
// }

// MODULE: platform(lib)()(common)
// FILE: jvm.kt
fun unused(): Foo = null!!

fun box(): String {
    val expect = 2 * 5 * 7
    val actual = common()
    Foo().javaLib()
    return if (expect == actual) "OK" else "FAIL $expect $actual"
}
