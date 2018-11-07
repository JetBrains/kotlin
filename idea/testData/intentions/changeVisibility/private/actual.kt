// SKIP_ERRORS_BEFORE
// SKIP_ERRORS_AFTER

// IS_APPLICABLE: false
expect class C {
    fun test()
}

<caret>actual class C {
    actual fun test() {}
}