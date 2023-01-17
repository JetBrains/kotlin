// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER

inline fun <reified T> areCopies(x: VArray<T>, y: VArray<T>): Boolean {
    if (x === y) return false
    if (x.size != y.size) return false
    for (i in 0 until x.size) if (x[i] != y[i]) return false
    return true
}

@JvmInline
value class IcInt(val x: Int)

fun box(): String {
    val vArrayInt = VArray(2) { it }
    if (!areCopies(vArrayInt, vArrayInt.clone())) return "Fail 1"

    val vArrayIcInt = VArray(2) { IcInt(it) }
    if (!areCopies(vArrayIcInt, vArrayIcInt.clone())) return "Fail 2"

    val vArrayStr = VArray(2) { "aba" }
    if (!areCopies(vArrayStr, vArrayStr.clone())) return "Fail 3"

    return "OK"
}