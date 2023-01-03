abstract class A : () -> Unit

object B : (String, Int) -> Long {
    override fun invoke(a: String, B: Int) = 23L
}

abstract class C : kotlin.Function1<Any, Int>

abstract class D : C()
