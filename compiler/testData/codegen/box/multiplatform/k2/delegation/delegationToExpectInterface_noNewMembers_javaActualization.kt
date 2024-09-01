// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-68517

// MODULE: common
// FILE: common.kt
expect interface Base {
    fun foo(a: Int): String
}

class DelegatedImpl<T>(val foo: Base) : Base by foo

// MODULE: platform()()(common)
// FILE: BaseJava.java
public interface BaseJava {
    String foo(int a);
}

// FILE: main.kt
actual typealias Base = BaseJava

class Impl : Base {
    override fun foo(a: Int): String = "OK"
}

fun box(): String {
    val x = DelegatedImpl<Int>(Impl())
    return x.foo(1)
}
