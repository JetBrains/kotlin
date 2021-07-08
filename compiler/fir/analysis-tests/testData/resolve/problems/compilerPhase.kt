interface CommonBackendContext

interface PhaserState<Data> {
    var depth: Int
}

interface PhaseConfig {
    val needProfiling: Boolean
}

inline fun <R, D> PhaserState<D>.downlevel(nlevels: Int, block: () -> R): R {
    depth += nlevels
    val result = block()
    depth -= nlevels
    return result
}

interface CompilerPhase<in Context : CommonBackendContext, Input, Output> {
    fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, input: Input): Output
}

class NamedCompilerPhase<in Context : CommonBackendContext, Data>(
    private val lower: CompilerPhase<Context, Data, Data>
) : CompilerPhase<Context, Data, Data> {
    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Data>, context: Context, input: Data): Data {
        // Expected: output: Data, Actual: output: Data?
        val output = if (phaseConfig.needProfiling) {
            runAndProfile(phaseConfig, phaserState, context, input)
        } else {
            phaserState.downlevel(1) {
                lower.invoke(phaseConfig, phaserState, context, input)
            }
        }
        runAfter(phaseConfig, phaserState, context, output)
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

    private fun runAfter(phaseConfig: PhaseConfig, phaserState: PhaserState<Data>, context: Context, output: Data) {

    }

    private fun runAndProfile(phaseConfig: PhaseConfig, phaserState: PhaserState<Data>, context: Context, source: Data): Data {

    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
}
