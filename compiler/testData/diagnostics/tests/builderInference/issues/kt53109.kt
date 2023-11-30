// ISSUE: KT-53109
// FIR_IDENTICAL
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = build {
        typeVariableConsumer = { consumeTargetType(it) }
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




class TargetType

fun consumeTargetType(value: TargetType) {}

class Buildee<TV> {
    var typeVariableConsumer: (TV) -> Unit = { storage = it }
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
