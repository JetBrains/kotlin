// ISSUE: KT-53639

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    val buildee = initializeAndBuild(
        { build { setTypeVariable(TargetType()) } },
        { placeholderExtensionInvokeOnBuildee() },
    )
    consumeTargetTypeBuildee(buildee)
    return "OK"
}




class TargetType

fun consumeTargetTypeBuildee(value: Buildee<TargetType>) {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = TargetType() as TV
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
