// SUGGESTED_NAMES: baz, paramBar, s
fun foo(paramBar: String) {

}

fun test() {
    val <caret>baz = ""
    foo(baz)
}