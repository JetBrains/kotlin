// FIR_IDENTICAL
// !LANGUAGE: +InlineClasses

package kotlin

annotation class JvmInline

@JvmInline
value class Test(val x: Int = 42)