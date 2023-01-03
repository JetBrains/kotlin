// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

@nativeSetter
fun toplevelFun(): Any = 0

<!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
val toplevelVal = 0

<!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
class Foo {}
