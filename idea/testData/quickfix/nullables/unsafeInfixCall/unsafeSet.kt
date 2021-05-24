// "Replace with safe (?.) call" "true"

operator fun Int.set(row: Int, column: Int, value: Int) {}
fun foo(arg: Int?) {
    arg<caret>[42, 13] = 0
}