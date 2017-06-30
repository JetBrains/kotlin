// WITH_RUNTIME

val ints = intArrayOf(1, 2, 3)
val objs = arrayOf(1, 2, 3)

val i1 = 1

fun box(): String {
    // NB partially const-folded in JVM BE
    if (!(1 in ints.indices)) return "Fail 1 in IntArray.indices"
    if (1 !in ints.indices) return "Fail 1 !in IntArray.indices"
    if (!(1 in objs.indices)) return "Fail 1 in Array.indices"
    if (1 !in objs.indices) return "Fail 1 !in Array.indices"

    if (!(i1 in ints.indices)) return "Fail i1 in IntArray.indices"
    if (i1 !in ints.indices) return "Fail i1 !in IntArray.indices"
    if (!(i1 in objs.indices)) return "Fail i1 in Array.indices"
    if (i1 !in objs.indices) return "Fail i1 !in Array.indices"

    return "OK"
}
