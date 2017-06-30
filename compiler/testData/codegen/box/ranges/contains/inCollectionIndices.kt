// WITH_RUNTIME

val xs = listOf(1, 2, 3)

val i1 = 1

fun box(): String {
    // NB partially const-folded in JVM BE
    if (!(1 in xs.indices)) return "Fail 1 in Collection.indices"
    if (1 !in xs.indices) return "Fail 1 !in Collection.indices"

    if (!(i1 in xs.indices)) return "Fail i1 in Collection.indices"
    if (i1 !in xs.indices) return "Fail i1 !in Collection.indices"

    return "OK"
}
