// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Foo(val a: Int) {
    fun member(): String = ""

    fun asResult() = a
}

fun <T> id(x: T): T = x
fun <T> T.idExtension(): T = this

fun Foo.extension() {}


fun test(f: Foo): String {
    id(f) // box
    id(f).idExtension() // box

    id(f).member() // box unbox
    id(f).extension() // box unbox

    val a = id(f) // box unbox
    val b = id(f).idExtension() // box unbox

    if (a.asResult() != 10) return "fail a"
    if (b.asResult() != 10) return "fail b"

    return "OK"
}

fun box(): String {
    val f = Foo(10)

    return test(f)
}
