// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

interface I {
    fun foo()
}

data class Pair<X, Y>(val fst: X, val snd: Y)

class A(f: Pair<Int, (I) -> Unit>? = null)

class B(f: ((I) -> Unit)? = null)

fun main() {
    val cond = true
    A(
        if (cond) {
            Pair(1, { baz -> baz.foo() })
        } else {
            null
        }
    )
    B(
        if (cond) {
            { baz -> baz.foo() }
        } else {
            null
        }
    )
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, functionalType, ifExpression, integerLiteral,
interfaceDeclaration, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, typeParameter */
