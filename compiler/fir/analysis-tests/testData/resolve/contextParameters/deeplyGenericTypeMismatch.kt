// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +DisableMaxTypeDepthFromInitialConstraints
// ISSUE: KT-81441
interface Dsl<F>

context(_: Dsl<F>)
fun <F> dslFun() {}

data object FalseNegative : Dsl<List<List<List<*>>>> {
    fun test() {
        <!NO_CONTEXT_ARGUMENT!>dslFun<!><Int>()
    }
}

fun FalseNegative.anotherFalseNegative() {
    <!NO_CONTEXT_ARGUMENT!>dslFun<!><Int>()
}

data object FailsAsExpected : Dsl<List<List<*>>> {
    fun test() {
        <!NO_CONTEXT_ARGUMENT!>dslFun<!><Int>()
    }
}

fun Dsl<List<List<List<*>>>>.failsAsExpected() {
    <!NO_CONTEXT_ARGUMENT!>dslFun<!><Int>()
}

/* GENERATED_FIR_TAGS: data, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
interfaceDeclaration, nullableType, objectDeclaration, starProjection, typeParameter */
