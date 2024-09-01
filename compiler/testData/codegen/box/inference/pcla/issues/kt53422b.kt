// ISSUE: KT-53422

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    stepByStepBuild(
        {
            it.concreteTypeMemberProperty
            TargetType()
        },
        {
            consumeTargetTypeBase(it)
        }
    )
    return "OK"
}




class ConcreteType
open class TargetTypeBase {
    val concreteTypeMemberProperty: ConcreteType = ConcreteType()
}
class TargetType: TargetTypeBase()

fun consumeTargetTypeBase(value: TargetTypeBase) {}

class Buildee<TV>

fun <PTV> stepByStepBuild(
    instructionsA: Buildee<PTV>.(PTV) -> PTV,
    instructionsB: Buildee<PTV>.(PTV) -> Unit,
): Buildee<PTV> {
    return Buildee<PTV>().apply { instructionsB(instructionsA(TargetType() as PTV)) }
}
