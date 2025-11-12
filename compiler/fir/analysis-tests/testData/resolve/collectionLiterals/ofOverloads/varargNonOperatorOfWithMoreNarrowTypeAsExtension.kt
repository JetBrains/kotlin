// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CollectionLiterals

open class P
class Q: P()

class A {
    companion object {
        operator fun of(vararg p: P): A = A()
    }

    class B
}

fun A.Companion.of(vararg q: Q) = A.B()

fun test() {
    val x: A = []
    val y: A = [P()]
    val z: A = [Q()]
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, localProperty,
objectDeclaration, operator, propertyDeclaration, vararg */
