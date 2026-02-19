// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class A {
    companion object {

    }
}

fun use(vararg a: Any?) = a

fun test() {
    use(use(A, null).toString())
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration,
outProjection, vararg */
