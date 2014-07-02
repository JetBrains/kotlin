import kotlin.reflect.KFunction0

class A {
    fun main() {
        val x = ::A

        x : KFunction0<A>
    }
}

class SomeOtherClass {
    fun main() {
        val x = ::A

        x : KFunction0<A>
    }
}
