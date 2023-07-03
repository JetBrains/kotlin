// COMPARE_WITH_LIGHT_TREE
// ISSUE: KT-40851

fun error(): Nothing = throw Exception()

<!UNREACHABLE_CODE{LT}!>class Some<!UNREACHABLE_CODE{PSI}!>()<!> {
    var x: Int
    val y: Int =<!> error()

    <!UNREACHABLE_CODE{LT}!>init {
        <!UNREACHABLE_CODE!>x = 1<!>;
    }
}<!>
