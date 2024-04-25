// DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

interface Base {
    fun base() {}
}
interface Base2
interface One : Base, Base2
interface Two : Base, Base2

object O1 : One
object O2 : Two

fun <S> intersect(vararg elements: S): S = TODO()

fun intersectAfterSmartCast(arg: Base, arg2: Base) = intersect(
    run {
        if (arg !is One) throw Exception()
        arg
    },
    run {
        if (arg2 !is Two) throw Exception()
        arg2
    }
)

fun <S> argOrFn(arg: S, fn: () -> S): S = TODO()

fun intersectArgWithSmartCastFromLambda(arg: One, arg2: Base) = argOrFn(arg) {
    if (arg2 !is Two) throw Exception()
    arg2
}

fun test() {
    intersectAfterSmartCast(O1, O2).<!UNRESOLVED_REFERENCE!>base<!>()
    intersectArgWithSmartCastFromLambda(O1, O2).<!UNRESOLVED_REFERENCE!>base<!>()
}
