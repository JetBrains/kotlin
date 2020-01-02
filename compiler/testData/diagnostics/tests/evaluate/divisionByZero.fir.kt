// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

val a1 = 0
val a2 = 1 / 0
val a3 = 1 / a1
val a4 = 1 / a2
val a5 = 2 * (1 / 0)

val a6 = 1.div(0)
val a7 = 1.div(a1)
val a8 = 1.div(a2)
val a9 = 2 * (1.div(0))

val b1: Byte = 1 / 0
@Ann(1 / 0) val b2 = 1

annotation class Ann(val i : Int)