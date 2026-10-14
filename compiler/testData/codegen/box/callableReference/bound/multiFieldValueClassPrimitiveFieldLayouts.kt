// LANGUAGE: +FullValueClasses
// ISSUE: KT-86207

value class IntPair(val x: Int, val y: Int)
value class FlippedIntPair(val y: Int, val x: Int)
value class DoublePair(val x: Double, val y: Double)
value class IntLong(val x: Int, val y: Long)
value class LongInt(val x: Long, val y: Int)

fun IntPair.render(): String = "I:${x + y}"
fun FlippedIntPair.render(): String = "FI:$x:$y"
fun DoublePair.render(): String = "D:${x == 1.5 && y == 2.5}"
fun IntLong.render(): String = "IL:$x:$y"
fun LongInt.render(): String = "LI:$x:$y"

fun box(): String {
    val intReceiver: () -> String = IntPair(1, 2)::render
    val doubleReceiver: () -> String = DoublePair(1.5, 2.5)::render

    val first = intReceiver()
    if (first != "I:3") return "FAIL int receiver"

    val flippedIntReceiver: () -> String = FlippedIntPair(y = 3, x = 4)::render
    val flipped = flippedIntReceiver()
    if (flipped != "FI:4:3") return "FAIL reversed Int/Int field order"

    val second = doubleReceiver()
    if (second != "D:true") return "FAIL double receiver"

    val intLongReceiver: () -> String = IntLong(5, 6L)::render
    val longIntReceiver: () -> String = LongInt(7L, 8)::render

    val third = intLongReceiver()
    if (third != "IL:5:6") return "FAIL Int/Long field order"

    val fourth = longIntReceiver()
    if (fourth != "LI:7:8") return "FAIL Long/Int field order"

    return "OK"
}
