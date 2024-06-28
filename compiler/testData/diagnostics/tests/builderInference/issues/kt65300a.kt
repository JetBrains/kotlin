// FIR_IDENTICAL
// ISSUE: KT-65300
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = build {
        val typeInfoSourceValue: Buildee<TargetType> = this
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




class TargetType

class Buildee<TV>

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
