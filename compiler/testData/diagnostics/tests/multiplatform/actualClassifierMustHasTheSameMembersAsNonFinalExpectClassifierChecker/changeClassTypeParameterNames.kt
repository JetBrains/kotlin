// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers
// MODULE: m1-common
// FILE: common.kt

open class Base<T> {
    open fun returnType(): T = null!!
    open fun parameterType(t: T) {}
    context(T)
    open fun contextReceiverType() {}
    open fun T.extensionReceiverType() {}
}

expect open class Foo<E> : Base<E>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo<R> : Base<R>() {
    override fun returnType(): R = null!!
    override fun parameterType(t: R) {}
    context(R)
    override fun contextReceiverType() {}
    override fun R.extensionReceiverType() {}
}
