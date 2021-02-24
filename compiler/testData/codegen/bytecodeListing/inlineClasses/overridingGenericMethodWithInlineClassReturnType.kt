// !LANGUAGE: +InlineClasses

interface A<T> {
    fun foo(): T
}

inline class Foo(val x: Long) : A<Foo> {
    override fun foo() = this
}