@Suppress("warnings")
class C {
    fun foo(p: String??) {
        // Make sure errors are not suppressed:
        <!VAL_REASSIGNMENT!>p<!> = ""
    }
}