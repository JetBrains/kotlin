// DUMP_IR
// DUMP_IR_DIFFERENCE: JVM
// LANGUAGE: +RichErrors
error class Foo

fun produceUnion() : String | Foo = "ok"
fun produceUnion2() : String | Foo = Foo()
fun produceUnion3() : String? | Foo = null

fun box(): String {
    produceUnion() as NonError
    produceUnion() as NonError?
    produceUnion() as? NonError
    produceUnion() as? NonError?

    try {
        produceUnion2() as NonError
        return "FAIL 1"
    } catch (e: ClassCastException) {
        // expected
    }
    try {
        produceUnion2() as NonError?
        return "FAIL 2"
    } catch (e: ClassCastException) {
        // expected
    }
    produceUnion2() as? NonError
    produceUnion2() as? NonError?

    try {
        produceUnion3() as NonError
        return "FAIL 3"
    } catch (e: ClassCastException) {
        // expected
    }
    produceUnion3() as NonError?
    produceUnion3() as? NonError
    produceUnion3() as? NonError?

    return "OK"
}
