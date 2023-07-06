// !LANGUAGE: +ProhibitAssigningSingleElementsToVarargsInNamedForm
// !DIAGNOSTICS: -UNUSED_PARAMETER

object X1
object X2

fun overloadedFun5(vararg ss: String) = X1
fun overloadedFun5(s: String, vararg ss: String) = X2

val test1 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>overloadedFun5<!>("")
val test2 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>overloadedFun5<!>("", "")
val test3: X2 = overloadedFun5(s = "", ss = <!ARGUMENT_TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>""<!>)
val test4: X1 = overloadedFun5(ss = <!ARGUMENT_TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>""<!>)
