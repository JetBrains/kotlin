// WITH_STDLIB

// MODULE: lib
annotation class LibAnn(val i: Int)

inline fun make(i: Int) = LibAnn(i)
inline fun eqViaAny(a: Any, b: Any) = a.equals(b)
inline fun hcViaAny(a: Any) = a.hashCode()
inline fun tsViaAny(a: Any) = a.toString()

// MODULE: main(lib)
fun box(): String {
    val a1 = make(1)
    val a2 = make(1)
    val a3 = make(2)

    if (!eqViaAny(a1, a2)) return "Fail1"
    if (hcViaAny(a1) != hcViaAny(a2)) return "Fail2"
    if (eqViaAny(a1, a3)) return "Fail3"

    val ts = tsViaAny(a1)
    if (ts.isEmpty() || !ts.contains("LibAnn")) return "Fail4"

    return "OK"
}