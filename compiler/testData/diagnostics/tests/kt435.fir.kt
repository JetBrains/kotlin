// COMPARE_WITH_LIGHT_TREE
fun Any.foo1() : (i : Int) -> Unit {
    return {}
}

fun test(a : Any) {
    a.foo1(<!NO_VALUE_FOR_PARAMETER{PSI}!>)<!>(<!NO_VALUE_FOR_PARAMETER{LT}!>)<!>
}
