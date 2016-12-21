// "Create subclass" "false"
// ACTION: Create test
// ACTION: Convert to sealed class
// ACTION: Rename file to My.kt

enum class <caret>My {
    SINGLE {
        override fun foo(): Int = 0
    };

    abstract fun foo(): Int
}
