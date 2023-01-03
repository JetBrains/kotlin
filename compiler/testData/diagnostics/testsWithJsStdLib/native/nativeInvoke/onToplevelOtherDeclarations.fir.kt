// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

@nativeInvoke
fun toplevelFun() {}

<!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
val toplevelVal = 0

<!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
class Foo {}
