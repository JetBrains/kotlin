package test

interface A {
    fun get(x : Int)
}

open class B(val a: A)

class C : B(object : A {
    override fun get(x : Int) {}
})

//package test
//public interface A defined in test
//public abstract fun get(x: kotlin.Int): kotlin.Unit defined in test.A
//value-parameter x: kotlin.Int defined in test.A.get
//public open class B defined in test
//public constructor B(a: test.A) defined in test.B
//value-parameter a: test.A defined in test.B.<init>
//public final class C : test.B defined in test
//public constructor C() defined in test.C
//local final class <no name provided> : test.A defined in test.C.<init>
//public constructor <no name provided>() defined in test.C.<init>.<no name provided>
//public open fun get(x: kotlin.Int): kotlin.Unit defined in test.C.<init>.<no name provided>
//value-parameter x: kotlin.Int defined in test.C.<init>.<no name provided>.get