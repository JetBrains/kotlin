// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

open class Base<T> {
    open fun foo(): T = null!!
}

expect open class Foo<E> : Base<E>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo<R> : Base<R>() {
    override fun foo(): R = null!!
}
