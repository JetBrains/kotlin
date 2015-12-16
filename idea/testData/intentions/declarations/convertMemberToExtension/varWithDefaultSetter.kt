// ERROR: Property must be initialized
// WITH_RUNTIME
// SKIP_ERRORS_AFTER
class Owner {
    var <caret>p: Int
        get() = 1
        set
}
