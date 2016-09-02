// "Remove redundant assignment" "false"
fun foo(): Int {
    var i = 1
    <caret>i = 2
    return i
}