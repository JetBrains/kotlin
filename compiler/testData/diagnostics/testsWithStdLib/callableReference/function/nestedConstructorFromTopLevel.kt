import kotlin.reflect.KFunction0

class A {
    class Nested
}

fun main() {
    val x = A::Nested

    x : KFunction0<A.Nested>
}
