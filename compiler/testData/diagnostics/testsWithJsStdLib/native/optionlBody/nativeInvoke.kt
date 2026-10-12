// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -DEPRECATION
<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke<!>
fun String.foo(): Int
<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke<!>
fun String.bar(): Int = definedExternally

external object O {
    @nativeInvoke
    fun foo()
    @nativeInvoke
    fun bar() { definedExternally }
}
