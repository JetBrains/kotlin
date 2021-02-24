import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class A {
    var bar: Int = 1
}

var bar = 1

fun foo1(x: KMutableProperty0<Int>) {}
fun foo2(x: KMutableProperty1<A, Int>) {}

fun main() {
    foo1(::bar)
    foo2(A::bar)
}
