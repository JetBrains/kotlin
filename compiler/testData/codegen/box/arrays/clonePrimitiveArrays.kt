// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

fun box(): String {
    val i = intArrayOf(1, 2)
    if (!(i contentEquals i.clone())) return "Fail int"
    if (i.clone() === i) return "Fail int identity"

    val j = longArrayOf(1L, 2L)
    if (!(j contentEquals j.clone())) return "Fail long"
    if (j.clone() === j) return "Fail long identity"

    val s = shortArrayOf(1.toShort(), 2.toShort())
    if (!(s contentEquals s.clone())) return "Fail short"
    if (s.clone() === s) return "Fail short identity"

    val b = byteArrayOf(1.toByte(), 2.toByte())
    if (!(b contentEquals b.clone())) return "Fail byte"
    if (b.clone() === b) return "Fail byte identity"

    val c = charArrayOf('a', 'b')
    if (!(c contentEquals c.clone())) return "Fail char"
    if (c.clone() === c) return "Fail char identity"

    val d = doubleArrayOf(1.0, -1.0)
    if (!(d contentEquals d.clone())) return "Fail double"
    if (d.clone() === d) return "Fail double identity"

    val f = floatArrayOf(1f, -1f)
    if (!(f contentEquals f.clone())) return "Fail float"
    if (f.clone() === f) return "Fail float identity"

    val z = booleanArrayOf(true, false)
    if (!(z contentEquals z.clone())) return "Fail boolean"
    if (z.clone() === z) return "Fail boolean identity"

    return "OK"
}
