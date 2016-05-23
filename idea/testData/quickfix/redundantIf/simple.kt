// "Remove redundant 'if' statement" "true"
fun bar(value: Int): Boolean {
    <caret>if (value % 2 == 0) {
        return true
    } else {
        return false
    }
}