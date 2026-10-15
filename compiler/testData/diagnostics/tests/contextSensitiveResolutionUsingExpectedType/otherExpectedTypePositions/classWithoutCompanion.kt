// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80943

enum class E {
    A, B
}

class A

val B: Int = 1

class Outer {
    class B

    fun test() {
        val a: E = A
        val b1: Int = B
        val b2: E = <!INITIALIZER_TYPE_MISMATCH!>B<!>
    }
}

fun negative() {
    val n: Int = <!INITIALIZER_TYPE_MISMATCH, NO_COMPANION_OBJECT!>A<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, integerLiteral, localProperty,
nestedClass, propertyDeclaration */
