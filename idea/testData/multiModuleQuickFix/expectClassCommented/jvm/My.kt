// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

/**
 * Dokka comment: class to be created as expect
 */
actual class <caret>My {
    // Helpful function
    actual fun foo(param: String): Int = 42

    /* Very helpful extension */
    actual fun String.bar(y: Double): Boolean = true

    /**
     * Dokka comment: Just does nothing
     */
    actual fun baz() {}

    /**
     * Dokka comment: Just does nothing
     *
     * @flag this parameter is just ignored
     */
    actual constructor(flag: Boolean) {}

    // Some immutable property
    actual val isGood: Boolean
        get() = true

    /* Interesting mutable property */
    actual var status: Int
        get() = 0
        set(value) {}

}