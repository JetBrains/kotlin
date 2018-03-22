// !LANGUAGE: +InlineClasses

inline class Foo(val x: Int) {
    fun member() {}
}

fun Foo.extension() {}
fun <T> T.genericExtension() {}

fun test(f: Foo?) {
    f?.member() // unbox
    f?.extension() // unbox
    f?.genericExtension()
}

// 0 INVOKESTATIC Foo\$Erased.box
// 2 INVOKEVIRTUAL Foo.unbox

// 0 valueOf
// 0 intValue