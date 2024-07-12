// ISSUE: KT-53639
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = initializeAndBuild(
        { build { setTypeVariable(TargetType()) } },
        { placeholderExtensionInvokeOnBuildee() },
    )
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




class TargetType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = null!!
}

fun <ETV> Buildee<ETV>.placeholderExtensionInvokeOnBuildee() {}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}

fun <PTV> initializeAndBuild(
    initializer: () -> Buildee<PTV>,
    instructions: Buildee<PTV>.() -> Unit
): Buildee<PTV> {
    return initializer().apply(instructions)
}
