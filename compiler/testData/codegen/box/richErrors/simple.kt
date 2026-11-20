// LANGUAGE: +RichErrors
// DUMP_IR
// IGNORE_IR_DESERIALIZATION_TEST: ANY
//   ^^^ KT-91653 IR: add serialization support for rich errors
error class Foo

fun produceUnion() : String | Foo = "OK"
fun acceptUnion(x: String? | Foo): String {
    return if (x is String) x else "FAIL"
}

fun box(): String {
    val u = produceUnion()
    return acceptUnion(u)
}
