// "Add non-null asserted (!!) call" "true"

fun foo(a: String?) {
    a<caret>.length
}