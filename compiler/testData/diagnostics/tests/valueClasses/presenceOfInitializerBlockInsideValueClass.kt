// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FIR_IDENTICAL

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Foo(val x: Int) {
    init {}

    init {
        val f = 1
    }
}
