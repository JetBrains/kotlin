// !DIAGNOSTICS: -UNUSED_PARAMETER

object Right
object Wrong

fun overloadedFun6(s1: String) = Right
fun overloadedFun6(s1: String, s2: String) = Wrong
fun overloadedFun6(s1: String, s2: String, s3: String) = Wrong
fun overloadedFun6(s: String, vararg ss: String) = Wrong

val test6: Right = overloadedFun6("")
