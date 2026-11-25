// LANGUAGE: +RichErrors
// DUMP_IR
// DUMP_IR_DIFFERENCE: JVM
error class E1
error class E2

fun box(): String {
    val _ = "" as (String | E1)
    val _ = E1() as (String | E1)
    try {
        val _ = E1() as (String | E2)
        return "FAIL 1"
    } catch (e: ClassCastException) {
        // expected
    }
    val _ = "" as (NonError | E1)
    val _ = E1() as (NonError | E1)
    try {
        val _ = E2() as (NonError | E1)
        return "FAIL 2"
    } catch (e: ClassCastException) {
        // expected
    }

    if (("" as? (String | E1)) !is String) return "FAIL 3"
    if ((E1() as? (String | E1)) !is E1) return "FAIL 4"
    if ((E1() as? (String | E2)) != null) return "FAIL 5"
    if (("" as? (NonError | E1)) == null) return "FAIL 6"
    if ((E1() as? (NonError | E1)) == null) return "FAIL 7"
    if ((E2() as? (NonError | E1)) != null) return "FAIL 8"

    return "OK"
}
