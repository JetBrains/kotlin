// LANGUAGE: +RichErrors
// DUMP_IR
// IGNORE_IR_DESERIALIZATION_TEST: ANY
//   ^^^ KT-91653 IR: add serialization support for rich errors
error class Foo

fun produceUnion() : String | Foo = "ok"
fun produceUnion2() : String | Foo = Foo()
fun produceUnion3() : String? | Foo = null

fun box(): String {
    if (produceUnion() !is NonError) return "FAIL 1"
    if (produceUnion2() is NonError) return "FAIL 2"
    if (produceUnion2() is NonError?) return "FAIL 3"
    if (produceUnion3() !is NonError?) return "FAIL 4"
    if (produceUnion3() is NonError) return "FAIL 5"
    return "OK"
}
