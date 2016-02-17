// "Specify type explicitly" "false"
// ERROR: This property must either have a type annotation, be initialized or be delegated
// ACTION: Convert member to extension
// ACTION: Convert property to function
// ACTION: Move to companion object

class My {
    val <caret>x
        get() {
            return 3.14
        }
}