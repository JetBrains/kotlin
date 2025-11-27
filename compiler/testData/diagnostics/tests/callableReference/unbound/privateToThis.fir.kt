// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE

class A<in T>(private val x: T) {
    fun <S> leak() = A<S>::<!INVISIBLE_REFERENCE!>x<!>
}

open class Base
class Child : Base()

fun main() {
    val y = A(Base())
    val ref = y.leak<Child>()
    // The following line would cause a runtime CCE in buggy compilers, but is not needed for frontend diagnostics
    // val v2 = ref(y)
}
