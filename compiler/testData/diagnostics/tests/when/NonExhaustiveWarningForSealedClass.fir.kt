sealed class S

object First : S()

class Derived(val s: String) : S()

object Last : S()

fun use(s: String) = s

fun foo(s: S) {
    when (s) {
        First -> {}
        is Derived -> use(s.s)
    }
}