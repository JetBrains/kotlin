// RUN_PIPELINE_TILL: BACKEND
// FILE: a.kt
object A {
    val O = object : B() {
        override val message = "expression expected"
    }
}

// FILE: b.kt
abstract class B {
    protected abstract val message: String
}