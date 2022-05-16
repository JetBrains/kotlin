// WITH_REFLECT
// TARGET_BACKEND: JVM
import kotlin.reflect.full.declaredFunctions

annotation class Anno(val value: String)

@JvmInline
value class A(val value: String)

abstract class B {
    @Anno(value = "K")
    abstract fun f(): A?
}

class C : B() {
    override fun f(): A? = A("O")
}

class D : B() {
    override fun f(): Nothing? = null
}

fun box(): String {
    val o = if ((D() as B).f() == null) (C() as B).f()!!.value else "Fail"

    val annotations = B::class.declaredFunctions.single().annotations
    val k = (annotations.single() as Anno).value

    return o + k
}
