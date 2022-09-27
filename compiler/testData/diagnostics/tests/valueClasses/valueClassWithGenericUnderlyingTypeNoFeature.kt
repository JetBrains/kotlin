// FIR_IDENTICAL
// !SKIP_JAVAC
// SKIP_TXT
// !LANGUAGE: +InlineClasses, -GenericInlineClassParameter

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Foo<T>(val x: <!UNSUPPORTED_FEATURE!>T<!>)
@JvmInline
value class FooNullable<T>(val x: <!UNSUPPORTED_FEATURE!>T?<!>)

@JvmInline
value class FooGenericArray<T>(val x: <!UNSUPPORTED_FEATURE!>Array<T><!>)
@JvmInline
value class FooGenericArray2<T>(val x: <!UNSUPPORTED_FEATURE!>Array<Array<T>><!>)


