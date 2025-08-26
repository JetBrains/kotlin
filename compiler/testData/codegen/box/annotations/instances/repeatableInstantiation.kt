// WITH_STDLIB
// TARGET_BACKEND: JVM_IR, WASM

@kotlin.annotation.Repeatable
annotation class Rep(val v: Int)

annotation class Holder(val reps: Array<Rep>)

fun box(): String {
    val r1 = Rep(1)
    val r2 = Rep(1)
    val r3 = Rep(2)

    if (r1 != r2) return "Fail1"
    if (r1.hashCode() != r2.hashCode()) return "Fail2"
    if (r1 == r3) return "Fail3"

    val h1 = Holder(arrayOf(Rep(1), Rep(2)))
    val h2 = Holder(arrayOf(Rep(1), Rep(2)))
    val h3 = Holder(arrayOf(Rep(2), Rep(1)))

    if (h1 != h2) return "Fail4"
    if (h1.hashCode() != h2.hashCode()) return "Fail5"
    if (h1 == h3) return "Fail6"

    return "OK"
}