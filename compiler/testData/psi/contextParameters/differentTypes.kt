// LANGUAGE: +ContextParameters
// FILE: Foo.kt
class Foo {
    context(@Anno _: @Anno suspend () -> Unit, _: suspend String.() -> Int)
    fun contextParameter() {

    }

    context(@Anno _: @Anno suspend () -> Unit, _: suspend String.() -> Int)
    val contextParameter2 get() = 0

    context(_: @Anno A & Any, _: B & Any)
    fun <A, B, C> (C & Any).dnnF() {

    }

    context(_: @Anno A & Any, _: B & Any)
    val <A, B, C> (C & Any).dnnP get() = 0

    context(@Anno _: @Anno suspend context(Int) () -> Unit, _: suspend context(String) () -> Boolean)
    val contextParameter3 get() = 0

    context(@Anno _: @Anno suspend context(Int) () -> Unit, _: suspend context(String) () -> Boolean)
    fun contextParameter4() {}

    val returnType: @Anno suspend context(Int) () -> Unit get() = {}
}

// FILE: A.kt
@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno
