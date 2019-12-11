// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_VARIABLE

fun test() {
    data class Pair<F, S>(val first: F, val second: S)
    val (x, y) =
            Pair(1,
                 if (1 == 1)
                     <!UNRESOLVED_REFERENCE!>Pair<String, String>::first<!>
                 else
                     <!UNRESOLVED_REFERENCE!>Pair<String, String>::second<!>)
}