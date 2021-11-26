// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class I(val i: Int)

abstract class A {
    abstract fun f(i: I): String
}

open class B : A() {
    override fun f(i: I): String = "OK"
}

class C : B() {
    override fun f(i: I): String = super.f(i)
}

fun box(): String {
    return C().f(I(0))
}
