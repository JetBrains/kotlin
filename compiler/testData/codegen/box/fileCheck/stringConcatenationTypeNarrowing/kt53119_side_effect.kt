// WITH_STDLIB
import kotlin.collections.*

val sideEffectsHolder = mutableListOf<Int>()
fun withSideEffect(param: Int): String? {
    sideEffectsHolder.add(param)
    return "Result $param"
}

fun box(): String {
    val result1 = "Result1 is: " + withSideEffect(1)
    if (result1 != "Result1 is: Result 1") return "FAIL 1: $result1"
    if (sideEffectsHolder != listOf(1)) return "FAIL 1 sideffects: $sideEffectsHolder"
    sideEffectsHolder.clear()
    val result2 = "Result2 is: " + withSideEffect(2) + "!"
    if (result2 != "Result2 is: Result 2!") return "FAIL 2: $result2"
    if (sideEffectsHolder != listOf(2)) return "FAIL 2 sideffects: $sideEffectsHolder"
    sideEffectsHolder.clear()
    val result3 = withSideEffect(31) + withSideEffect(32)
    if (result3 != "Result 31Result 32") return "FAIL 3: $result3"
    if (sideEffectsHolder != listOf(31,32)) return "FAIL 3 sideffects: $sideEffectsHolder"
    sideEffectsHolder.clear()
    val result4 = withSideEffect(41) + withSideEffect(42) + withSideEffect(43)
    if (result4 != "Result 41Result 42Result 43") return "FAIL 4: $result4"
    if (sideEffectsHolder != listOf(41,42,43)) return "FAIL 4 sideffects: $sideEffectsHolder"
    return "OK"
}
