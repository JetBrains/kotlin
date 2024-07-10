// IGNORE_BACKEND: ANY
// ^^^ Muted because accessing object literal is considered as a visibility violation. To be fixed in KT-69802.

// FILE: A.kt
class A {
    private fun privateMethod() = "OK"

    internal inline fun internalInlineMethod() = object {
        fun run() = privateMethod()
    }.run()
}

// FILE: main.kt
fun box(): String {
    return A().internalInlineMethod()
}
