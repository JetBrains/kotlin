// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    fun f()
    val v: String
}

expect class Bar {
    fun g()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo(actual open val v: String) {
    actual open fun f() {}
}

actual typealias Bar = JavaBar

// FILE: JavaBar.java

public class JavaBar {
    public void g() {}
}
