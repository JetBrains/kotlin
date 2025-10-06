// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-81441
interface Dsl<F>

context(_: Dsl<F>)
fun <F> dslFun() {}

data object FalseNegative : Dsl<List<List<List<*>>>> {
    fun test() {
        dslFun<Int>()
    }
}

fun FalseNegative.anotherFalseNegative() {
    dslFun<Int>()
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
