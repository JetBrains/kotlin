// "Create subclass" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION

class Container {
    protected companion object {
        open class <caret>Base
    }
}
