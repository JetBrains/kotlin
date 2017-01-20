interface T {
    fun foo(): Unit
}

open class A : T {
    override fun foo() {}
}

interface B : T

class C : A(), B
class D : B, A()
class E : A(), B, T
class F : B, A(), T
class G : A(), T, B
class H : B, T, A()
class I : T, A(), B
class J : T, B, A()

fun box(): String {
    C().foo()
    D().foo()
    E().foo()
    F().foo()
    G().foo()
    H().foo()
    I().foo()
    J().foo()

    return "OK"
}
