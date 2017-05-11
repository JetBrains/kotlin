sealed class S

object First : S()

class Derived(val s: String) : S()

object Last : S()

fun use(s: String) = s

fun foo(s: S) {
    <!NON_EXHAUSTIVE_WHEN_ON_SEALED_CLASS!>when<!> (s) {
        First -> {}
        is Derived -> use(<!DEBUG_INFO_SMARTCAST!>s<!>.s)
    }
}