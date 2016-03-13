// "Create subclass" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION

class Container {
    private open class <caret>Base {
        open var x: String = ""

        open fun foo(): String = ""
    }

    private class BaseImpl : Base()
}
