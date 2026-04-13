// RUN_PIPELINE_TILL: BACKEND
object A {
    object B {
        object C
    }
}

val a = A.B.C

/* GENERATED_FIR_TAGS: nestedClass, objectDeclaration, propertyDeclaration */
