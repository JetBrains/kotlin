// IGNORE_BACKEND: WASM, JS_IR, JS_IR_ES6, NATIVE
// WITH_STDLIB
// LANGUAGE: +MultiPlatformProjects

// MODULE: common()()()
// TARGET_PLATFORM: Common
// FILE: common.kt

// open class CommonBase {
//     fun commonFakeOverride(): Int = 2
//     open fun commonOverride(): Int = null!! // 7
// }

@OptIn(ExperimentalMultiplatform::class)
@kotlin.jvm.ImplicitlyActualizedByJvmDeclaration
expect class Foo() {
    fun foo(): Int // 5
}

fun common(): Int = Foo().foo() //* Foo().commonFakeOverride() * Foo().commonOverride()

// MODULE: lib()()()
// FILE: lib.kt
class Foo {
    fun foo(): Int = 5
    // override fun commonOverride(): Int = 7
}

// open class CommonBase {
//     fun commonFakeOverride(): Int = 2
//     open fun commonOverride(): Int = null!! // 7
// }

// MODULE: platform(lib)()(common)
// FILE: jvm.kt
fun box(): String {
    val expect = 2 * 5 * 7
    val actual = common()
    return if (expect == actual) "OK" else "FAIL $expect $actual"
}
