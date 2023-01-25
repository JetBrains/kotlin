// WITH_STDLIB
// SKIP_TXT

fun box(): String {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>buildList<!> {
        val foo = { first() }
        add(0, <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>)
    }
    return "OK"
}
