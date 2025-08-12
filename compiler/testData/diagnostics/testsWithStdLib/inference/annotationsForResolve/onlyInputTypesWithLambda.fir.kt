// RUN_PIPELINE_TILL: FRONTEND
@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <@kotlin.internal.OnlyInputTypes T> expect(expected: T, block: () -> T) {}

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <@kotlin.internal.OnlyInputTypes T> expect(block: () -> T) {}

fun returnNullableString(): String? = ""
fun returnString(): String = ""
fun returnAny(): Any = ""
fun returnNothing(): Nothing = null!!

@Suppress("UNREACHABLE_CODE")
fun test() {
    // T := String, must be OK
    expect(returnString()) {
        returnString()
    }

    // T := String?, must be OK
    expect(returnNullableString()) {
        returnNullableString()
    }

    // T := String?, must be OK
    expect(null) {
        returnNullableString()
    }

    // T := String?, must be FAIL
    <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>expect<!>(null) {
        returnString()
    }

    // T := Any, must be OK
    expect(returnAny()) {
        Unit
    }

    // T := Unit, must be OK
    expect(returnNothing()) {
        Unit
    }

    // T := Any, must be OK
    expect(returnAny()) {
    }

    // T := Unit, must be OK
    <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>expect<!>(returnNothing()) {
    }

    // T := String, must be OK
    expect {
        returnString()
    }

    // T := Unit, must be OK
    expect {
        Unit
    }

    // T := Unit, must be OK
    <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>expect<!> {
    }

    // T := Unit?, must be FAIL
    <!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>expect<!>(null) {
    }

    // T := Any, must be FAIL
    arrayOf<Int>().<!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>indexOf<!> {
    }

    // T := Any, must be OK
    arrayOf<Any>().indexOf {
    }

    // T := () -> Any, must be OK
    arrayOf<() -> Any>().indexOf {
    }

    // T := () -> Unit, must be OK
    arrayOf<() -> Nothing>().indexOf {
    }

    // T := () -> Unit?, must be FAIL
    arrayOf<() -> Nothing?>().<!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>indexOf<!> {
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, typeParameter */
