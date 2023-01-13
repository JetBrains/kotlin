interface I
open class C

fun completed(): String = "..."

fun <T> incomplete(): T = null!!

fun <T : I> incompatibleI(): T = null!!
fun <T : C> incompatibleC(): T = null!!

val p = false

fun expectUnit(x: Unit) = x

fun test1() = run {
    if (p) return@run
    completed() // ok, not returned
}

fun test2() = run {
    if (p) return@run
    incomplete() // ? either uninferred T or Unit
}

fun test3() = <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>run<!> {
    if (p) return@run
    incompatibleI() // ? either uninferred T or error (Unit </: I)
}

fun test4() = <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>run<!> {
    if (p) return@run
    incompatibleC() // ? either uninferred T or error (Unit </: C)
}

fun main() {
    // all ok
    expectUnit(test1())
    expectUnit(test2())
    expectUnit(test3())
    expectUnit(test4())
}
