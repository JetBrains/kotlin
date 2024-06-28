// ISSUE: KT-63841
// CHECK_TYPE_WITH_EXACT

class TargetType { fun targetTypeMemberFunction() {} }
class DifferentType

fun test() {
    val targetTypeBuildee = build {
        var variable = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>getTypeVariable()<!>
        variable = TargetType()
        variable.targetTypeMemberFunction()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(<!TYPE_MISMATCH("Buildee<TargetType>; Buildee<TargetType?>"), TYPE_MISMATCH("Buildee<TargetType?>; Buildee<TargetType>")!>targetTypeBuildee<!>)

    val differentTypeBuildee = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        var variable = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>getTypeVariable()<!>
        variable = DifferentType()
        variable.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>targetTypeMemberFunction<!>()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<DifferentType>>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>differentTypeBuildee<!>)

    val anyBuildee = build {
        var variable = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>getTypeVariable()<!>
        variable = TargetType()
        variable.targetTypeMemberFunction()
        variable = DifferentType()
        variable.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>targetTypeMemberFunction<!>()
        variable = TargetType()
        variable.targetTypeMemberFunction()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<Any>>(<!TYPE_MISMATCH("Buildee<Any>; Buildee<TargetType>"), TYPE_MISMATCH("Buildee<TargetType>; Buildee<Any>")!>anyBuildee<!>)
}




class Buildee<TV> {
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
