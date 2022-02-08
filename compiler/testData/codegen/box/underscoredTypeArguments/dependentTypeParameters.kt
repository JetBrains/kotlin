// !LANGUAGE: +PartiallySpecifiedTypeArguments
// WITH_STDLIB
// TARGET_BACKEND: JVM

abstract class SomeClass<T> {
    abstract fun execute() : T
}

class SomeImplementation : SomeClass<String>() {
    override fun execute(): String = "Test"
}

object Runner {
    inline fun <reified S: SomeClass<T>, T> run() : T {
        return S::class.java.newInstance().execute()
    }
}

fun box(): String {
    val s = Runner.run<SomeImplementation, _>() // T is inferred to String
    return "OK"
}