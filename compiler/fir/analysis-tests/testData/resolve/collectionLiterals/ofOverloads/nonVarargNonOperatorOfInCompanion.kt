// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

class P

class A<T> {
    companion object {
        operator fun of(vararg p: P): A<String> = A<String>()
        fun of(p: P): A<Char> = A<Char>()
    }
}

fun test() {
    val x: A<String> = []
    val y: A<String> = [P()]

    val z: A<Char> <!INITIALIZER_TYPE_MISMATCH!>=<!> []
    val t: A<Char> <!INITIALIZER_TYPE_MISMATCH!>=<!> [P()]
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, objectDeclaration,
operator, propertyDeclaration, vararg */
