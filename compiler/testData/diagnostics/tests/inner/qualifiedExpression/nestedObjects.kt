// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
object A {
    object B {
        object C
    }
}

val a = A.B.C

/* GENERATED_FIR_TAGS: nestedClass, objectDeclaration, propertyDeclaration */
