// WITH_STDLIB
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

open class CommonBase {
    fun commonFakeOverride(): Int = 2
    open fun commonOverride(): Int = null!! // 7
}

@OptIn(ExperimentalMultiplatform::class)
@kotlin.jvm.ImplicitlyActualizedByJvmDeclaration
expect class Foo() : CommonBase {
    fun foo(): Int // 5
}

fun common(): Int = Foo().foo() * Foo().commonFakeOverride() * Foo().commonOverride()

// MODULE: lib()()()
// FILE: lib.java
class Foo : CommonBase, JvmBase {
    fun foo(): Int = 5
    override fun commonOverride(): Int = 7
}

// MODULE: platform(lib)()(common)
// FILE: jvm.kt
fun box(): String {
    val expect = 2 * 3 * 5 * 7
    val actual = common() * Foo().jvmFakeOverride()
    return if (expect == actual) "OK" else "FAIL $expect $actual"
}

interface JvmBase {
    fun jvmFakeOverride(): Int = 3
}
