interface A<T> {
    fun f(x: T): T
}

open class B {
    open fun f(x: String): String = x
}

open class C : B(), A<String>

class D : C()

fun box(): String {
    return (D() as A<String>).f("OK")
}

// class D should not have an additional bridge
// 1 public synthetic bridge f\(Ljava/lang/Object;\)Ljava/lang/Object;
// 1 bridge
