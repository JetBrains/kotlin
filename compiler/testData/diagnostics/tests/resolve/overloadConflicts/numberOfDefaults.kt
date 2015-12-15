// !DIAGNOSTICS: -UNUSED_PARAMETER

object Right
object Wrong

fun overloadedFun(c: Any = "", b: String = "", f: Any = "") = Right
fun overloadedFun(b: Any = "", c: Any = "", e: String = "", x: String = "", y: String = "") = Wrong

val test: Right = overloadedFun(b = "")

