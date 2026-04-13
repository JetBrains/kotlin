// RUN_PIPELINE_TILL: BACKEND
class A() {
    val a: Int = 1
    fun a(): Int = 2
}

class B() {
    fun b(): Int = 2
    val b: Int = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, primaryConstructor, propertyDeclaration */
