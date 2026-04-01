import kotlin.jvm.internal.*

val p0 = ""
var mp0 = ""

class A {
    val p1 = ""
    var mp1 = ""

    val String.p2 get() = this
    var String.mp2 get() = this; set(v) {}
}

fun main() {
    Reflection.property0(::p0 as PropertyReference0)
    Reflection.mutableProperty0(::mp0 as MutablePropertyReference0)
    Reflection.property1(A::p1 as PropertyReference1)
    Reflection.mutableProperty1(A::mp1 as MutablePropertyReference1)
}
