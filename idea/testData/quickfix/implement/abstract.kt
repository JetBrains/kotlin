// "Implement abstract class" "true"
// WITH_RUNTIME
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION

private abstract class <caret>Base {
    abstract var x: Int

    abstract fun toInt(arg: String): Int
}