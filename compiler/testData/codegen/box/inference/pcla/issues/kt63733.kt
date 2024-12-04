// ISSUE: KT-63733

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

fun BoundedBuildee<TargetType>.setBoundedTypeVariable(arg: DifferentType) {}

fun box(): String {
    boundedBuild<TargetType> {
        setBoundedTypeVariable(TargetType())
        setBoundedTypeVariable(DifferentType())
    }
    boundedBuild {
        setBoundedTypeVariable(TargetType())
        setBoundedTypeVariable(DifferentType())
    }
    return "OK"
}




open class TargetTypeBase
class TargetType: TargetTypeBase()
class DifferentType

class BoundedBuildee<BTV: TargetTypeBase> {
    fun setBoundedTypeVariable(value: BTV) { storage = value }
    private var storage: BTV = TargetType() as BTV
}

fun <PBTV: TargetTypeBase> boundedBuild(instructions: BoundedBuildee<PBTV>.() -> Unit): BoundedBuildee<PBTV> {
    return BoundedBuildee<PBTV>().apply(instructions)
}
