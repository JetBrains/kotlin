// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline class Foo(val x: Int) {
    @JvmName("other")
    fun simple() {}
}

@JvmName("bad")
fun bar(f: Foo) {}

@JvmName("good")
fun baz(r: Result<Int>) {}