// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION
@file:Suppress("OPT_IN_USAGE")

<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
fun toplevelFun()<!> {}

<!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
val toplevelVal = 0

<!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
class Foo {}
