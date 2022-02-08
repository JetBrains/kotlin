// !LANGUAGE: +InlineClasses
// WITH_STDLIB

inline class Foo(val a: Any)

@JvmName("bar")
fun bar(f: Foo) {}

@JvmName("baz")
fun baz(r: Result<Int>) {}

@JvmName("test")
fun returnsInlineClass() = Foo(1)

@JvmName("test")
@Suppress("RESULT_CLASS_IN_RETURN_TYPE")
fun returnsKotlinResult(a: Result<Int>): Result<Int> = a

class C {
    @JvmName("test")
    fun returnsInlineClass() = Foo(1)

    @JvmName("test")
    @Suppress("RESULT_CLASS_IN_RETURN_TYPE")
    fun returnsKotlinResult(a: Result<Int>): Result<Int> = a
}

@JvmName("extensionFun")
fun Foo.extensionFun() {}
