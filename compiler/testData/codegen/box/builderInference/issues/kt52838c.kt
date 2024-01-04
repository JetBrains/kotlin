// ISSUE: KT-52838
// JVM_ABI_K1_K2_DIFF: KT-64738

// IGNORE_BACKEND_K2: WASM
// REASON: That still doesn't work properly with PCLA, but accidentally don't fails for other tests but WASM
// (see KT-52838 for tracking and same-named diagnostic test)

fun box(): String {
    build {
        this as DerivedBuildee<*>
        getTypeVariable()
    }
    return "OK"
}




open class Buildee<TV> {
    fun getTypeVariable(): TV = storage
    private var storage: TV = Any() as TV
}

class DerivedBuildee<TA>: Buildee<TA>()

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return DerivedBuildee<PTV>().apply(instructions)
}
