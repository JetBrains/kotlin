// !WITH_NEW_INFERENCE
fun text() {
    "direct:a" to "mock:a"
    "direct:a" on {it.body == "<hello/>"} to "mock:a"
    "direct:a" on {it -> it.body == "<hello/>"} to "mock:a"
    bar <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>{<!>1}
    bar <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>{<!><!UNRESOLVED_REFERENCE!>it<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> 1}
    bar {it, <!UNUSED_ANONYMOUS_PARAMETER!>it1<!> -> it}

    bar1 {1}
    bar1 {it + 1}

    bar2 {<!TYPE_MISMATCH!><!>}
    bar2 {1}
    bar2 {<!UNRESOLVED_REFERENCE!>it<!>}
    bar2 <!NI;TYPE_MISMATCH!>{<!OI;CANNOT_INFER_PARAMETER_TYPE, OI;EXPECTED_PARAMETERS_NUMBER_MISMATCH!>it<!> -> <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!>}<!>
}

fun bar(<!UNUSED_PARAMETER!>f<!> :  (Int, Int) -> Int) {}
fun bar1(<!UNUSED_PARAMETER!>f<!> :  (Int) -> Int) {}
fun bar2(<!UNUSED_PARAMETER!>f<!> :  () -> Int) {}

infix fun String.to(<!UNUSED_PARAMETER!>dest<!> : String) {

}

infix fun String.on(<!UNUSED_PARAMETER!>predicate<!> :  (s : URI) -> Boolean) : URI {
    return URI(this)
}

class URI(val body : Any) {
    infix fun to(<!UNUSED_PARAMETER!>dest<!> : String) {}
}
