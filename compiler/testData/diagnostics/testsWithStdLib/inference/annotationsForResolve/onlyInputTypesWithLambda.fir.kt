// RUN_PIPELINE_TILL: BACKEND
@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <@kotlin.internal.OnlyInputTypes T> expect(expected: T, block: () -> T) {}

fun foo() {
    expect(null, { arrayOf<Int>().minOrNull() })
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, stringLiteral, typeParameter */
