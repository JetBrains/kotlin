// LANGUAGE: +RichErrors
// DUMP_IR
error class E1
error class E2

val x1: String | E1 | E2 = ""
val x2: String | E1 | E2 = E2()
val x3: String? | E1 | E2 = null

fun box(): String {
    if (x1 is (E1 | E2)) return "FAIL 1"
    if (x2 is (String | E1)) return "FAIL 1"
    if (x3 !is (Nothing? | E1 | E2)) return "FAIL 2"

    return "OK"
}
