// "Replace with safe (?.) call" "true"
// WITH_RUNTIME

fun foo(list: List<String>?) {
    var s = ""
    s = list[0]<caret>
}