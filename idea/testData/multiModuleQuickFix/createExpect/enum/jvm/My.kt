// "Create expected enum class in common module testModule_Common" "true"
// DISABLE-ERRORS

actual enum class <caret>My {
    FIRST,
    SECOND,
    LAST;

    val num: Int get() = 42
}