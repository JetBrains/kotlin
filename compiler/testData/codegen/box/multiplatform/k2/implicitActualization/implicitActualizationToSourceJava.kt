// IGNORE_BACKEND: WASM, JS_IR, JS_IR_ES6, NATIVE, JVM_IR
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

// MODULE: platform()()(common)
// FILE: Foo.java
public class Foo extends CommonBase implements JvmBase {
    public int foo() { return 5; }
    @Override public int commonOverride() { return 7; }
}

// FILE: JvmBase.java
public interface JvmBase {
    default public int jvmFakeOverride() { return 3; }
}

// FILE: jvm.kt
fun box(): String {
    val expect = 2 * 3 * 5 * 7
    val actual = common() * Foo().jvmFakeOverride()
    return if (expect == actual) "OK" else "FAIL $expect $actual"
}
