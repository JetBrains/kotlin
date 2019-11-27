// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
package whats.the.difference

fun iarray(vararg a : Int) = a // BUG
val IntArray.indices: IntRange get() = IntRange(0, lastIndex())
fun IntArray.lastIndex() = size - 1

fun box() : String {
    val vals = iarray(789, 678, 567, 456, 345, 234, 123, 12)
    val diffs = HashSet<Int>()
    for (i in vals.indices)
        for (j in i..vals.lastIndex())
             diffs.add(vals[i] - vals[j])
    val size = diffs.size

    if (size != 8) return "Fail $size"
    return "OK"
}
