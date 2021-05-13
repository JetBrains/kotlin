// "Add non-null asserted (!!) call" "true"
// WITH_RUNTIME
var i = 0

fun foo(s: String?) {
    i = s<caret>.length
}
