// ISSUE: KT-63733
// CHECK_TYPE_WITH_EXACT

fun BoundedBuildee<TargetType>.setBoundedTypeVariable(arg: DifferentType) {}

fun test() {
    boundedBuild<TargetType> {
        setBoundedTypeVariable(TargetType())
        setBoundedTypeVariable(DifferentType())
    }
    val buildee = boundedBuild {
        setBoundedTypeVariable(TargetType())
        setBoundedTypeVariable(<!TYPE_MISMATCH("TargetTypeBase; DifferentType")!>DifferentType()<!>)
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<BoundedBuildee<TargetType>>(<!TYPE_MISMATCH("BoundedBuildee<TargetType>; BoundedBuildee<Any>"), TYPE_MISMATCH("BoundedBuildee<Any>; BoundedBuildee<TargetType>")!>buildee<!>)
}




open class TargetTypeBase
class TargetType: TargetTypeBase()
class DifferentType

class BoundedBuildee<BTV: TargetTypeBase> {
    fun setBoundedTypeVariable(value: BTV) { storage = value }
    private var storage: BTV = null!!
}

fun <PBTV: TargetTypeBase> boundedBuild(instructions: BoundedBuildee<PBTV>.() -> Unit): BoundedBuildee<PBTV> {
    return BoundedBuildee<PBTV>().apply(instructions)
}
