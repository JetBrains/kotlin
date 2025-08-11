// RUN_PIPELINE_TILL: FRONTEND
@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <@kotlin.internal.OnlyInputTypes T> expect(expected: T, block: () -> T) {}

fun foo() {
    <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>expect<!>(null, { arrayOf<Int>().minOrNull() })
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, stringLiteral, typeParameter */
