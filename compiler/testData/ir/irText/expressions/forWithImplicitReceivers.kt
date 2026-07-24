// WITH_STDLIB
// DUMP_IR_DIFFERENCE: JVM
//   K/JVM invokes `println (kotlin.Int)` instead of `println (message: kotlin.Any?)`

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
