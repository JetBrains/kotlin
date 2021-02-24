// FIR_IDENTICAL
// !SKIP_JAVAC
// FIR_IDENTICAL
// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Foo(val x: Int) {
    init {}

    init {
        val f = 1
    }
}
