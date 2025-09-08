// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-58055

fun <T> produce(arg: () -> T): T = arg()

fun main() {
    produce {
        suspend fun() {} // CCE
    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, lambdaLiteral, nullableType,
typeParameter */
