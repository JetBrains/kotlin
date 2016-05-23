// "Remove redundant 'if' statement" "true"
fun bar() {
    <caret>if (value % 2 == 0) {
        return true
    } else {
        return false
    }
}