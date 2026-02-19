// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-70894

// MODULE: common
expect class Foo1() {
    override fun equals(other: Any?): Boolean
}

open class Base {
}

fun test() : String {
    val x = Foo1()
    return if (x.equals(x)) "OK" else "FAIL"
}

// MODULE: jvm()()(common)
// FILE: Foo.java

public class Foo extends Base {
}

// FILE: Jvm.kt
actual typealias Foo1 = Foo

fun box() = test()
