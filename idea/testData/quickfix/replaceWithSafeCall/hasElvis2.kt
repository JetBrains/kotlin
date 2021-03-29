// "Replace with safe (this?.) call" "true"
// WITH_RUNTIME
var i = 0

fun foo(a: String?) {
    a.run {
        i = <caret>length ?: 0
    }
}
