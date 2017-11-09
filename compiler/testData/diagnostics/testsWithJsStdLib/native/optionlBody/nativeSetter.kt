// !DIAGNOSTICS: -DEPRECATION
@nativeSetter
fun String.foo(n: Int, v: Any)
@nativeSetter
fun String.bar(n: Int, v: Any) {}


external class C {
    @nativeSetter
    fun foo(d: Double, v: Any): Any
    @nativeSetter
    fun bar(d: Double, v: Any): Any = definedExternally
}