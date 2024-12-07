// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-57709
// CHECK_TYPE_WITH_EXACT
// WITH_STDLIB

fun test() {
    val buildee = build {
        setTypeVariable(Any())
        <!STUB_TYPE_IN_RECEIVER_CAUSES_AMBIGUITY!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, OVERLOAD_RESOLUTION_AMBIGUITY, OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES!>consumeBuildeeReceiver<!>()<!>
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<Any>>(buildee)
}




class DifferentType

fun consumeAny(value: Any) {}

fun consumeDifferentType(value: DifferentType) {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

@JvmName("consumeAnyBuildeeReceiver")
fun Buildee<Any>.consumeBuildeeReceiver() {
    consumeAny(getTypeVariable())
}
@JvmName("consumeDifferentTypeBuildeeReceiver")
fun Buildee<DifferentType>.consumeBuildeeReceiver() {
    consumeDifferentType(getTypeVariable())
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
