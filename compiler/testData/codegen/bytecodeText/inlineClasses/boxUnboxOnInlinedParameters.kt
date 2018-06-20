// !LANGUAGE: +InlineClasses

inline class Foo(val a: Int)

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

// 2 INVOKESTATIC Foo\$Erased.box
// 1 INVOKEVIRTUAL Foo.unbox

// 0 valueOf
// 0 intValue