// IGNORE_BACKEND_FIR: JVM_IR
operator fun IntArray.set(index: Long, elem: Int) { this[index.toInt()] = elem }
operator fun IntArray.get(index: Long) = this[index.toInt()]

fun box(): String {
    var l = IntArray(1)
    l[0.toLong()] = 4
    l[0.toLong()] += 6
    return if (l[0.toLong()] == 10) "OK" else "Fail"
}
