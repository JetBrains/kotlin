// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
fun interface IFoo {
    fun foo(): String
}

fun foo(iFoo: IFoo) = iFoo.foo()

open class C1 {
    open fun test() = foo { "O" }
}

class C2 : C1() {
    override fun test() = foo { "K" }
}

fun box() =
    C1().test() + C2().test()
