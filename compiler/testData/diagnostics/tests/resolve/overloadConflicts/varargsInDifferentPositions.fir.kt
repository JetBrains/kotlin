// !LANGUAGE: +ProhibitAssigningSingleElementsToVarargsInNamedForm
// !DIAGNOSTICS: -UNUSED_PARAMETER

object X1
object X2

fun overloadedFun(arg: String, vararg args: String) = X1
fun overloadedFun(arg: String, vararg args: String, flag: Boolean = true) = X2

val test1a: X1 = overloadedFun("", "")
val test1b: X1 = <!NONE_APPLICABLE!>overloadedFun<!>("", args = "")
val test1c: X2 = overloadedFun("", "", "", flag = true)

