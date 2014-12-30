nativeSetter
fun String.foo(n: Int, v: Any)
nativeSetter
fun String.bar(n: Int, v: Any) {}


native
class C {
    nativeSetter
    fun foo(d: Double, v: Any): Any
    nativeSetter
    fun bar(d: Double, v: Any): Any = noImpl
}