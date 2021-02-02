// !WITH_NEW_INFERENCE
fun text() {
    "direct:a" to "mock:a"
    "direct:a" on {it.body == "<hello/>"} to "mock:a"
    "direct:a" on {it -> it.body == "<hello/>"} to "mock:a"
    bar {<!ARGUMENT_TYPE_MISMATCH!>1<!>}
    bar {<!ARGUMENT_TYPE_MISMATCH!><!UNRESOLVED_REFERENCE!>it<!> + 1<!>}
    bar {it, it1 -> it}

    bar1 {1}
    bar1 {it + 1}

    <!INAPPLICABLE_CANDIDATE!>bar2<!> {}
    bar2 {1}
    bar2 {<!UNRESOLVED_REFERENCE!>it<!>}
    bar2 {it -> <!ARGUMENT_TYPE_MISMATCH!>it<!>}
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
