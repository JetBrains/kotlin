// !DIAGNOSTICS: -UNUSED_PARAMETER

public class A {
    public operator fun get(vararg attrs : Pair<String, String>) : A = this
}
operator fun String.unaryPlus() : A = A()
operator fun A.div(s : String) : A = A()

fun test() {
    (+"node2" / "node3" / "zzz") ["attr" to "value", "a2" to "v2"]
}

//---------
class B {
    public operator fun get(s : String, q : String) : B = this
    public operator fun get(s : Pair<String, String>) : B = this
    public operator fun invoke(q : B.() -> Unit) : B = this
}
val x = B()["a", "v"]["a" to "b"] {} ["q" to "p"] // does not parses around {}

//from library
data class Pair<out A, out B> (val first: A, val second: B)
infix fun <A,B> A.to(that: B) = Pair(this, that)