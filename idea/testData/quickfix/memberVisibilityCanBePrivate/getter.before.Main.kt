// "Add 'private' modifier" "false"
// ACTION: Convert to secondary constructor
// ACTION: Create test
// ACTION: Move to class body

class My(val <caret>parameter: Int) {
    val other = parameter
}