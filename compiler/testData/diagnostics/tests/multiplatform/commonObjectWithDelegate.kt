// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

interface I {
    val f: (Boolean) -> String
}

class C(override val f: (Boolean) -> String) : I

object O : I by C(
    f = { flag ->
        when (flag) {
            true -> "OK"
            false -> "Not OK"
        }
    }
)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

fun bar() = O.f(true)

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, inheritanceDelegation,
interfaceDeclaration, lambdaLiteral, objectDeclaration, override, primaryConstructor, propertyDeclaration */
