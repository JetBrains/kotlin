// !LANGUAGE: +ProhibitAssigningSingleElementsToVarargsInNamedForm
// !DIAGNOSTICS: -UNUSED_PARAMETER

object X1
object X2

fun overloadedFun5(vararg ss: String) = X1
fun overloadedFun5(s: String, vararg ss: String) = X2

val test1 = overloadedFun5("")
val test2 = overloadedFun5("", "")
val test3: X2 = <!INAPPLICABLE_CANDIDATE!>overloadedFun5<!>(s = "", ss = "")
val test4: X1 = <!INAPPLICABLE_CANDIDATE!>overloadedFun5<!>(ss = "")