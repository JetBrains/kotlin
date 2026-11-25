// LANGUAGE: +RichErrors
// DUMP_IR
// IGNORE_IR_DESERIALIZATION_TEST: ANY
//   ^^^ KT-91653 IR: add serialization support for rich errors
error class E1
error class E2

val x1: String | E1 | E2 = ""
val x2: String | E1 | E2 = E2()
val x3: String? | E1 | E2 = null

fun box(): String {
    if (x1 is (E1 | E2)) return "FAIL 1"
    if (x1 !is (NonError | E1 | E2)) return "FAIL 2"
    if (x2 is (String | E1)) return "FAIL 3"
    if (x2 is (NonError | E1)) return "FAIL 4"
    if (x3 !is (Nothing? | E1 | E2)) return "FAIL 5"

    return "OK"
}
