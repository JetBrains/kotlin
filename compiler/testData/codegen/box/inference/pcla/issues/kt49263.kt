// ISSUE: KT-49263

// IGNORE_BACKEND: ANY
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// REASON: red code (see corresponding diagnostic test)
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

fun box(): String {
    buildPostponedTypeVariable {
        consumeTargetType(this)
    }
    return "OK"
}




class TargetType

fun consumeTargetType(value: TargetType) {}

fun <PTV> buildPostponedTypeVariable(block: PTV.() -> Unit): PTV {
    return (TargetType() as PTV).apply(block)
}
