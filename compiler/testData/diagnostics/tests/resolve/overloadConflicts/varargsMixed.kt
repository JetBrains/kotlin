// LANGUAGE: +ProhibitAssigningSingleElementsToVarargsInNamedForm
// DIAGNOSTICS: -UNUSED_PARAMETER

object X1
object X2

fun overloadedFun5(vararg ss: String) = X1
fun overloadedFun5(s: String, vararg ss: String) = X2

val test1 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>overloadedFun5<!>("")
val test2 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>overloadedFun5<!>("", "")
val test3: X2 = <!NONE_APPLICABLE!>overloadedFun5<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>s<!> = "", <!DEBUG_INFO_MISSING_UNRESOLVED!>ss<!> = "")
val test4: X1 = overloadedFun5(ss = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR, TYPE_MISMATCH!>""<!>)
