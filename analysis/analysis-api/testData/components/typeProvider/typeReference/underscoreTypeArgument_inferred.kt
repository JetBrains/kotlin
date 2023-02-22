// example from https://kotlinlang.org/docs/generics.html#underscore-operator-for-type-arguments
// modified to avoid using reflection (::class.java)

abstract class SomeClass<T> {
    abstract fun execute() : T
}

class SomeImplementation : SomeClass<String>() {
    override fun execute(): String = "Test"
}

class OtherImplementation : SomeClass<Int>() {
    override fun execute(): Int = 42
}

object Runner {
    inline fun <reified S: SomeClass<T>, T> run(instance: S) : T {
        return instance.execute()
    }
}

fun test() {
    val i = SomeImplementation()
    // T is inferred as String because SomeImplementation derives from SomeClass<String>
    val s = Runner.run<_, _>(i)
    assert(s == "Test")

    val j = OtherImplementation()
    // T is inferred as Int because OtherImplementation derives from SomeClass<Int>
    val n = Runner.run<_, <caret>_>(j)
    assert(n == 42)
}
