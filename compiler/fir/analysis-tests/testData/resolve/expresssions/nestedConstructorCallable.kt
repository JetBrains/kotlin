// RUN_PIPELINE_TILL: BACKEND
class A {
    class Nested

    fun main() {
        val x = ::Nested
        val y = A::Nested
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, localProperty, nestedClass,
propertyDeclaration */
