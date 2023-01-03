// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

@nativeGetter
fun toplevelFun(): Any = 0

<!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
val toplevelVal = 0

<!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
class Foo {}
