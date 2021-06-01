// "Add non-null asserted (!!) call" "false"

fun foo(arg: String?) {
    if (arg == null) {
        arg<caret>.length
    }
}