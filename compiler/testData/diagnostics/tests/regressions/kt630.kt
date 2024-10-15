// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// KT-630 Bad type inference

fun <T : Any> T?.sure() : T = this!!

val x = "lala".sure()
val s : String = x
