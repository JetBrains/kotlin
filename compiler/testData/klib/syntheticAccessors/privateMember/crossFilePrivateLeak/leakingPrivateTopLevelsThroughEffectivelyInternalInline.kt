// FILE: A.kt
private var name: String = "John"
private fun greet(name: String) = "Hello, $name!"

internal class A {
    inline fun inlineFunction() {
        greet(name)
        name = "Mary"
        greet(name)
    }
}

// FILE: main.kt
fun box(): String {
    A().inlineFunction()
    return "OK"
}
