// "Replace scope function with safe (?.) call" "true"
// WITH_RUNTIME
var i = 0

fun foo(a: String?) {
    i = a.run {
        length<caret>
    }
}