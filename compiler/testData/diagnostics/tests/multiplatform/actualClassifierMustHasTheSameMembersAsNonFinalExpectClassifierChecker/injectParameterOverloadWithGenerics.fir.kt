// MODULE: m1-common
// FILE: common.kt

open class Base<T> {
    open fun foo(t: T) {}
}

expect open class Foo<R> : Base<R>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo<R>() : Base<R>() {
    fun <T> foo(t: T) {}
}
