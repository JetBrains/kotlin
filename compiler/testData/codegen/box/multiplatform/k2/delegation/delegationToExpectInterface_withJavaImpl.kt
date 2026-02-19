// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR

// MODULE: common
// FILE: common.kt
expect interface Base {
    fun foo(): String
    val a: String
}

class DelegatedImpl(val foo: Base) : Base by foo

// MODULE: platform()()(common)
// FILE: JavaImpl.java
public class JavaImpl implements Base {
    @Override
    public String foo() {
        return "O";
    }

    @Override
    public String getA() {
        return "K";
    }
}

// FILE: platform.kt
actual interface Base {
    actual fun foo(): String
    actual val a: String
}

fun box(): String {
    val x = DelegatedImpl(JavaImpl())
    return x.foo() + x.a
}
