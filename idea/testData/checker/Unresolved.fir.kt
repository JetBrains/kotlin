package unresolved

class Pair<A, B>(a: A, b: B)

fun testGenericArgumentsCount() {
    val p1: <error descr="[WRONG_NUMBER_OF_TYPE_ARGUMENTS] 2 type arguments expected for unresolved/Pair">Pair<Int></error> = Pair(2, 2)
    val p2: <error descr="[WRONG_NUMBER_OF_TYPE_ARGUMENTS] 2 type arguments expected for unresolved/Pair">Pair</error> = Pair(2, 2)
}

fun testUnresolved() {
    if (<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: a">a</error> is String) {
        val s = <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: a">a</error>
    }
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>(<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: a">a</error>)
    val s = "s"
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>(s)
    foo1(<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: i">i</error>)
    s.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>()

    when(<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: a">a</error>) {
        is Int -> <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: a">a</error>
        is String -> <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: a">a</error>
        else -> <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: a">a</error>
    }

    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: hasNext"><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: iterator"><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: next">for (j in <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: collection">collection</error>) {
       var i: Int = j
       i += 1
       foo1(j)
    }</error></error></error>
}

fun foo1(i: Int) {}
