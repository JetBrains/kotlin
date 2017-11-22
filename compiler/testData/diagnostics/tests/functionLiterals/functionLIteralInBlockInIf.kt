// !WITH_NEW_INFERENCE
// !CHECK_TYPE
fun test() {
    val a = if (true) {
        val x = 1
        ({ <!NI;UNRESOLVED_REFERENCE!>x<!> })
    } else {
        { <!NI;UNUSED_EXPRESSION!>2<!> }
    }
    <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!> <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>checkType<!> {  <!NI;UNRESOLVED_REFERENCE!>_<!><() -> Int>() }
}