// LANGUAGE: +CompanionBlocksAndExtensions
// MODULE: lib
// FILE: A.kt
class A {
    companion {
        fun foo() = "O"
        var bar: String = ""
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    A.bar = "K"
    return A.foo() + A.bar
}
