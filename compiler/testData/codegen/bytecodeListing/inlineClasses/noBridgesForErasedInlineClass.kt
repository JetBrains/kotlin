// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

interface A<T> {
    fun foo(a: T)
}

inline class Foo(val x: Long) : A<Foo> {
    override fun foo(a: Foo) {}
}