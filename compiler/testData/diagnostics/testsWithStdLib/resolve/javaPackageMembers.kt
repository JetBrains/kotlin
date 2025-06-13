// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun fn(): Nothing = throw java.lang.RuntimeException("oops")

val x: Nothing = throw java.lang.RuntimeException("oops")

class SomeClass {
    fun method() {
        throw java.lang.AssertionError("!!!")
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaFunction, propertyDeclaration, stringLiteral */
