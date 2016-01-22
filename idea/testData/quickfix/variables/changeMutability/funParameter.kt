// "Make variable mutable" "false"
// ERROR: Val cannot be reassigned
fun fun1(i: Int) {
    <caret>i = 2
}