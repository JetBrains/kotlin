// ISSUE: KT-63841
// CHECK_TYPE_WITH_EXACT

class TargetType { fun targetTypeMemberFunction() {} }
class DifferentType

fun test() {
    val targetTypeBuildee = build {
        var variable = getTypeVariable()
        variable = TargetType()
        variable.targetTypeMemberFunction()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(targetTypeBuildee)

    val differentTypeBuildee = build {
        var variable = getTypeVariable()
        variable = DifferentType()
        variable.<!UNRESOLVED_REFERENCE!>targetTypeMemberFunction<!>()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<DifferentType>>(differentTypeBuildee)

    val anyBuildee = build {
        var variable = getTypeVariable()
        variable = TargetType()
        variable.targetTypeMemberFunction()
        variable = <!ASSIGNMENT_TYPE_MISMATCH!>DifferentType()<!>
        variable.targetTypeMemberFunction()
        variable = TargetType()
        variable.targetTypeMemberFunction()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<Any>>(<!ARGUMENT_TYPE_MISMATCH!>anyBuildee<!>)
}




class Buildee<TV> {
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
