// !WITH_NEW_INFERENCE
fun text() {
    "direct:a" to "mock:a"
    "direct:a" on {it.body == "<hello/>"} to "mock:a"
    "direct:a" on {it -> it.body == "<hello/>"} to "mock:a"
    bar <!TYPE_MISMATCH{NI}!><!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>{<!>1}<!>
    bar <!TYPE_MISMATCH{NI}!><!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>{<!><!UNRESOLVED_REFERENCE!>it<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{OI}, DEBUG_INFO_MISSING_UNRESOLVED{NI}!>+<!> 1}<!>
    bar {it, it1 -> it}

    bar1 {1}
    bar1 {it + 1}

    bar2 <!TYPE_MISMATCH{NI}!>{<!TYPE_MISMATCH!><!>}<!>
    bar2 {1}
    bar2 <!TYPE_MISMATCH{NI}!>{<!UNRESOLVED_REFERENCE!>it<!>}<!>
    bar2 <!TYPE_MISMATCH{NI}!>{<!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>it<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{OI}, TYPE_MISMATCH{NI}!>it<!>}<!>
}

fun bar(f :  (Int, Int) -> Int) {}
fun bar1(f :  (Int) -> Int) {}
fun bar2(f :  () -> Int) {}

infix fun String.to(dest : String) {

}

infix fun String.on(predicate :  (s : URI) -> Boolean) : URI {
    return URI(this)
}

class URI(val body : Any) {
    infix fun to(dest : String) {}
}
