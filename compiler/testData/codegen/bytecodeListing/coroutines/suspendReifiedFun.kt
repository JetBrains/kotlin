// IGNORE_BACKEND: JVM_IR
// TODO: KT-37088
// COMMON_COROUTINES_TEST
// WITH_RUNTIME

open class AbstractStuff() {
    inline suspend fun<reified T> hello(value: T): T = println("Hello, ${T::class}").let { value }
}

class Stuff: AbstractStuff() {
    suspend fun foo() = hello(40)
}
