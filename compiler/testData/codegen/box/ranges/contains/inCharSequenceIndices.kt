// WITH_RUNTIME

val cs = "123"

val i1 = 1

fun box(): String {
    // NB partially const-folded in JVM BE
    if (!(1 in cs.indices)) return "Fail 1 in CharSequence.indices"
    if (1 !in cs.indices) return "Fail 1 !in CharSequence.indices"

    if (!(i1 in cs.indices)) return "Fail i1 in CharSequence.indices"
    if (i1 !in cs.indices) return "Fail i1 !in CharSequence.indices"

    return "OK"
}
