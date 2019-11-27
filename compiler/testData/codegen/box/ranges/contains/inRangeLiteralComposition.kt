// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val i1 = 1
val i2 = 2
val i3 = 3

fun box(): String {
    if (!(i2 in i1 .. i3 && i1 !in i2 .. i3)) return "Fail 1 &&"
    if (!(i2 in i1 .. i3 || i1 !in i2 .. i3)) return "Fail 1 ||"

    // const-folded in JVM BE
    if (!(2 in 1 .. 3 && 1 !in 2 .. 3)) return "Fail 2 &&"
    if (!(2 in 1 .. 3 || 1 !in 2 .. 3)) return "Fail 2 ||"

    val xs = listOf(1, 2, 3)
    if (!(1 in xs && 10 !in xs)) return "Fail 3 &&"
    if (!(1 in xs || 10 !in xs)) return "Fail 3 ||"

    val iarr = intArrayOf(1, 2, 3)
    if (!(1 in iarr && 10 !in iarr)) return "Fail 4 &&"
    if (!(1 in iarr || 10 !in iarr)) return "Fail 4 ||"

    if (!("b" in "a" .. "c" && "a" !in "b" .. "c")) return "Fail 5 &&"
    if (!("b" in "a" .. "c" || "a" !in "b" .. "c")) return "Fail 5 ||"

    return "OK"
}