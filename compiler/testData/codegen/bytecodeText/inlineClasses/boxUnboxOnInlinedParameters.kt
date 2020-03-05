// !LANGUAGE: +InlineClasses

// FILE: utils.kt

inline class Foo(val a: Int)

// FILE: test.kt

fun <T> id(x: T): T = x
inline fun <T> inlinedId(x: T): T = x

fun <T> T.idExtension(): T = this
inline fun <T> T.inlinedIdExtension(): T = this

fun test(f: Foo) {
    inlinedId(f)
    inlinedId(f).idExtension() // box

    f.inlinedIdExtension()

    val a = inlinedId(f).idExtension() // box unbox
    val b = inlinedId(f).inlinedIdExtension()
}

// @TestKt.class:
// 2 INVOKESTATIC Foo\.box
// 1 INVOKEVIRTUAL Foo.unbox
// 0 valueOf
// 0 intValue