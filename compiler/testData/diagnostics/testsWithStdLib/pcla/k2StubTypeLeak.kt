// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// DUMP_INFERENCE_LOGS: FIXATION

fun test_1() {
    sequence {
        constrain(this)
        yieldAll(mk()) // Will be completed in partial mode, due to builder inference

        Unit
    }
}

fun constrain(t: Inv<String>) {}

fun <U> sequence(block: Inv<U>.() -> Unit): U = null!!

interface Inv<T> {
    fun <S: T> yieldAll(seq: Inv<S>)
}

fun <K> mk(): K = TODO()

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, thisExpression, typeConstraint, typeParameter, typeWithExtension */
