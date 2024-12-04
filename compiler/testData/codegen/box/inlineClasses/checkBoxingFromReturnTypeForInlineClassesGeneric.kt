// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T: Int>(val a: T) {
    fun member(): String = ""

    fun asResult() = a
}

fun <T> id(x: T): T = x
fun <T> T.idExtension(): T = this

fun <T: Int> Foo<T>.extension() {}


fun <T: Int> test(f: Foo<T>): String {
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
