// ISSUE: KT-55281
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        this as DerivedBuildee<*>
        consumeNullableAny(getTypeVariable())
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>checkExactType<!><<!CANNOT_INFER_PARAMETER_TYPE!>Buildee<Any?><!>>(buildee)
}




class TargetType

fun consumeNullableAny(value: Any?) {}

open class Buildee<TV> {
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

class DerivedBuildee<TA>: Buildee<TA>()

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return DerivedBuildee<PTV>().apply(instructions)
}
