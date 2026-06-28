// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
object A {
    val a = 1
    val b = B.a
}

object B {
    val a = 2
}

/* GENERATED_FIR_TAGS: integerLiteral, objectDeclaration, propertyDeclaration */
