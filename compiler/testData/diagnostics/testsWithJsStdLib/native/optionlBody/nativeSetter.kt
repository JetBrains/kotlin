// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -DEPRECATION
<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeSetter<!>
fun String.foo(n: Int, v: Any)
<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeSetter<!>
fun String.bar(n: Int, v: Any) {}


external class C {
    @nativeSetter
    fun foo(d: Double, v: Any): Any
    @nativeSetter
    fun bar(d: Double, v: Any): Any = definedExternally
}
