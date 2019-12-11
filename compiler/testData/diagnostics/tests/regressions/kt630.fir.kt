// KT-630 Bad type inference

fun <T : Any> T?.sure() : T = this!!

val x = "lala".sure()
val s : String = x
