// MODULE: m1-common
// FILE: common.kt

open class Base {
    fun <T> foo(t: T) {}
}

expect open class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    fun <T : Comparable<T>> foo(t: T) {}
}
