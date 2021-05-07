// FILE: Some.kt

interface A<T>
inline fun <reified T> foo() {
    object : A<T> {}
}

// FILE: Main.kt
fun main(args: Array<String>) {
    //Breakpoint!
    println("hello")
}

// EXPRESSION: foo<String>()
// RESULT: instance of A<String>

