// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTICS_FULL_TEXT

class P

class A {
    companion object {
        operator fun of(vararg p: P): A = A()
        fun of(vararg p: Int): B = B()
    }

    class B
}

fun test() {
    val x: A = []
    val y: A = [P()]
    val z: A <!INITIALIZER_TYPE_MISMATCH!>=<!> <!OPERATOR_MODIFIER_REQUIRED!>[42]<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, objectDeclaration,
operator, propertyDeclaration, stringLiteral, vararg */
