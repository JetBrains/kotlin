// "Create subclass" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION

class Container {
    private companion object {
        open class <caret>Base
    }
}
