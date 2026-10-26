// LANGUAGE: +RichErrors
// DUMP_IR
error class Foo

fun produceUnion() : String | Foo = "ok"
fun produceUnion2() : String | Foo = Foo()
fun produceAny() : Any = "ok"
fun produceErrorAsAny() : Any = Foo()

fun box(): String {
    if (produceUnion()|.length != 2) return "FAIL 1"
    if (produceUnion2()|.length !is Foo) return "FAIL 2"
    if (produceAny()|.toString() != "ok") return "FAIL 3"
    if (produceErrorAsAny()|.toString() !is Foo) return "FAIL 4"
    return "OK"
}
