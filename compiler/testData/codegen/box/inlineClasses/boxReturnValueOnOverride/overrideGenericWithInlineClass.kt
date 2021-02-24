inline class Marker(val i: Int)

interface I<T> {
    fun foo(i: Marker) : T
}

inline class IC(val a: Any)

class C : I<IC> {
    override fun foo(i: Marker): IC = IC("OK")
}

fun box(): String {
    val i: I<IC> = C()
    val foo: IC = i.foo(Marker(0))
    if (foo.a != "OK") return "FAIL 1"
    val foo1: IC = C().foo(Marker(0))
    if (foo1.a != "OK") return "FAIL 2"
    return "OK"
}
