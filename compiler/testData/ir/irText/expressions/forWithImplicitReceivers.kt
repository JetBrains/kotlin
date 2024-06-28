// WITH_STDLIB
// IGNORE_BACKEND: JS_IR

// KT-61141: `println (message: kotlin.Any?)` instead of `println (message: kotlin.Int)`
// IGNORE_BACKEND: NATIVE

object FiveTimes

class IntCell(var value: Int)

interface IReceiver {
    operator fun FiveTimes.iterator() = IntCell(5)
    operator fun IntCell.hasNext() = value > 0
    operator fun IntCell.next() = value--
}

fun IReceiver.test() {
    for (i in FiveTimes) {
        println(i)
    }
}
