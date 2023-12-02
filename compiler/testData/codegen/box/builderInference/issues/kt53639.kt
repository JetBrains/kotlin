// ISSUE: KT-53639

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code in K1 (see corresponding diagnostic test)
// REASON: compile-time failure in K2/JVM (java.lang.IllegalStateException: Cannot serialize error type: ERROR CLASS: Cannot infer argument for type parameter PTV)
// REASON: compile-time failure in K2/Native, K2/WASM, K2/JS (org.jetbrains.kotlin.backend.common.linkage.issues.IrDisallowedErrorNode: Class found but error nodes are not allowed)

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
