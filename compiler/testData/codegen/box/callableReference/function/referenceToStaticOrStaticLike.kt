// LANGUAGE: +CompanionBlocksAndExtensions

class A<T> {
    companion {
        fun f1() = Unit
    }
    companion object {
        fun f2() = Unit
    }
    class f5(val param: String)
}

companion fun A.f3(param: Int) = Unit
fun A.Companion.f4() = Unit

fun box(): String {
    A::f1
    (A::f2)()
    A::f3.invoke(42)
    val ref: () -> Unit = A::f4
    return (A::f5).invoke("OK").param
}
