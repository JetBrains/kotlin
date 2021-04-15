fun foo(x: Int) {}
fun foo(y: String) {}

fun <T> bar(f: (T) -> Unit) {}

fun test() {
    bar(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>)
}
