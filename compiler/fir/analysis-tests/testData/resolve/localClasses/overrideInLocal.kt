// RUN_PIPELINE_TILL: BACKEND
// FILE: a.kt
fun main() {
     class Local : B() {
        override val message = "expression expected"
    }
}

// FILE: b.kt
abstract class B {
    protected abstract val message: String
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, override, propertyDeclaration, stringLiteral */
