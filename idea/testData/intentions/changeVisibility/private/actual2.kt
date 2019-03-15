// SKIP_ERRORS_BEFORE
// SKIP_ERRORS_AFTER

// IS_APPLICABLE: false
expect class C {
    fun test()
}

actual class C {
    <caret>actual fun test() {}
}