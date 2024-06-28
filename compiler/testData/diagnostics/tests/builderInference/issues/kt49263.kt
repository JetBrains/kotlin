// ISSUE: KT-49263
// CHECK_TYPE_WITH_EXACT

fun test() {
    val targetType = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>buildPostponedTypeVariable<!> {
        <!DEBUG_INFO_MISSING_UNRESOLVED!>consumeTargetType<!>(this)
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<TargetType>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>targetType<!>)
}




class TargetType

fun consumeTargetType(value: TargetType) {}

fun <PTV> buildPostponedTypeVariable(block: PTV.() -> Unit): PTV {
    return null!!
}
