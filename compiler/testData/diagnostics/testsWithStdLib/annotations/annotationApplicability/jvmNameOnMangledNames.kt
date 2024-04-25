// FIR_IDENTICAL
// LANGUAGE: +InlineClasses, -JvmInlineValueClasses
// DIAGNOSTICS: -UNUSED_PARAMETER

inline class Foo(val x: Int) {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("other")<!>
    fun simple() {}
}

@JvmName("bad")
fun bar(f: Foo) {}

@JvmName("good")
fun baz(r: Result<Int>) {}

@JvmName("test")
fun returnsInlineClass() = Foo(1)

@JvmName("test")
fun returnsKotlinResult(a: Result<Int>): Result<Int> = a

class C {
    @JvmName("test")
    fun returnsInlineClass() = Foo(1)

    @JvmName("test")
    fun returnsKotlinResult(a: Result<Int>): Result<Int> = a
}

@JvmName("extensionFun")
fun Foo.extensionFun() {}
