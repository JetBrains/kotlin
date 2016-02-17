// "Create subclass" "false"
// ACTION: Create test

enum class <caret>My {
    SINGLE {
        override fun foo(): Int = 0
    };

    abstract fun foo(): Int
}
