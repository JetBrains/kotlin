// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
fun toplevelFun()<!> {}

<!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
val toplevelVal = 0

<!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
class Foo {}