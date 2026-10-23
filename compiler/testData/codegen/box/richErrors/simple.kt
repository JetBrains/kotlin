// LANGUAGE: +RichErrors
// DUMP_IR
error class Foo

fun produceUnion() : String | Foo = "OK"
fun acceptUnion(x: String? | Foo): String {
    return if (x is String) x else "FAIL"
}

fun box(): String {
    val u = produceUnion()
    return acceptUnion(u)
}
