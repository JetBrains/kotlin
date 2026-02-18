// FILE: lib.kt
interface SomeInterface<T>

object Container {
    private inline fun <reified T> someMethod() = object : SomeInterface<T> { }
    class SomeClass : SomeInterface<SomeClass> by someMethod()
}

// FILE: main.kt
fun box(): String {
    Container.SomeClass()
    return "OK"
}
