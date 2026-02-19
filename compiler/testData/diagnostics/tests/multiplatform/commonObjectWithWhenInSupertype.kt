// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class C(val f: (Boolean) -> String)

object O : C(
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

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, objectDeclaration, primaryConstructor,
propertyDeclaration */
