// "Add non-null asserted (!!) call" "true"
// DISABLE-ERRORS

fun foo(arg: String?) {
    if (arg == null) {
        arg<caret>.length
    }
}