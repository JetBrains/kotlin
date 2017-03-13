interface T {
    fun foo(): Unit
}

open class A : T {
    override fun foo(): Unit {}
}

class B : A(), T
class C : T, A()

interface U : T
class D : U, A()
class E : A(), U
class F : U, T, A()
class G : T, U, A()
class H : U, A(), T
class I : T, A(), U
class J : A(), U, T
class K : A(), T, U

fun box(): String {
    B().foo()
    C().foo()
    D().foo()
    E().foo()
    F().foo()
    G().foo()
    H().foo()
    I().foo()
    J().foo()
    K().foo()

    return "OK"
}
