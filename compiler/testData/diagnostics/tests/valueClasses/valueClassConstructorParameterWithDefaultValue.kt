// FIR_IDENTICAL
// !LANGUAGE: +InlineClasses

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Test(val x: Int = 42)