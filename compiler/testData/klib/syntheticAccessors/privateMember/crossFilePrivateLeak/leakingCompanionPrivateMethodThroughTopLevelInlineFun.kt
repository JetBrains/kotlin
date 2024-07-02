// IGNORE_BACKEND: ANY

// FILE: A.kt
class A {
    companion object{
        private fun privateMethod() = "OK"
    }
}

// the function calls INVISIBLE_MEMBER
internal inline fun internalInlineMethod() = A.privateMethod()

// FILE: main.kt
fun box(): String {
    return internalInlineMethod()
}
