// ISSUE: KT-49263

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code (see corresponding diagnostic test)

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
