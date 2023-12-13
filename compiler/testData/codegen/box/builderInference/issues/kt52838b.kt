// ISSUE: KT-52838

// IGNORE_LIGHT_ANALYSIS
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        this as DerivedBuildee<*>
        getTypeVariable()
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
