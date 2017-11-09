// "Replace with safe (?.) call" "true"
// WITH_RUNTIME

fun foo(bar: Int?) {
    var i: Int = 1
    i = bar +<caret> 1
}