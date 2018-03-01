// !LANGUAGE: +InlineClasses

inline class Foo(val a: Int)

fun <T> id(x: T): T = x
inline fun <T> inlinedId(x: T): T = x

fun <T> T.idExtension(): T = this
inline fun <T> T.inlinedIdExtension(): T = this

fun test(f: Foo) {
    inlinedId(f) // box
    inlinedId(f).idExtension() // box

    f.inlinedIdExtension() // box

    val a = inlinedId(f).idExtension() // box unbox
    val b = inlinedId(f).inlinedIdExtension() // box unbox
}

// 5 INVOKESTATIC Foo\$Erased.box
// 2 INVOKEVIRTUAL Foo.unbox

// 0 valueOf
// 0 intValue