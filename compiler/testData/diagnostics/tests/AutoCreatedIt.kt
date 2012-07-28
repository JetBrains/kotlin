fun text() {
    "direct:a" to "mock:a"
    "direct:a" on {it.body == "<hello/>"} to "mock:a"
    "direct:a" on {it -> it.body == "<hello/>"} to "mock:a"
    bar <!TYPE_MISMATCH!>{1}<!>
    bar <!TYPE_MISMATCH!>{<!UNRESOLVED_REFERENCE!>it<!> + 1}<!>
    bar {it, it1 -> it}

    bar1 {1}
    bar1 {it + 1}

    bar2 <!TYPE_MISMATCH!>{<!TYPE_MISMATCH!><!>}<!>
    bar2 {1}
    bar2 {<!UNRESOLVED_REFERENCE!>it<!>}
    bar2 <!TYPE_MISMATCH!>{<!CANNOT_INFER_PARAMETER_TYPE!>it<!> -> it}<!>
}

fun bar(<!UNUSED_PARAMETER!>f<!> :  (Int, Int) -> Int) {}
fun bar1(<!UNUSED_PARAMETER!>f<!> :  (Int) -> Int) {}
fun bar2(<!UNUSED_PARAMETER!>f<!> :  () -> Int) {}

fun String.to(<!UNUSED_PARAMETER!>dest<!> : String) {

}

fun String.on(<!UNUSED_PARAMETER!>predicate<!> :  (s : URI) -> Boolean) : URI {
    return URI(this)
}

class URI(val body : Any) {
    fun to(<!UNUSED_PARAMETER!>dest<!> : String) {}
}
