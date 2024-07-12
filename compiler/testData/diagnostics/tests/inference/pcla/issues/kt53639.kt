// ISSUE: KT-53639
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>initializeAndBuild<!>(
        { build <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ setTypeVariable(TargetType()) }<!> },
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ placeholderExtensionInvokeOnBuildee() }<!>,
    )
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(<!TYPE_MISMATCH("Buildee<TargetType>; Buildee<Any?>"), TYPE_MISMATCH("Buildee<Any?>; Buildee<TargetType>")!>buildee<!>)
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
