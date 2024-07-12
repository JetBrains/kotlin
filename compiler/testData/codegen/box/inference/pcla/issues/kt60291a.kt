// ISSUE: KT-60291
// WITH_STDLIB

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    selectBuildee(
        build { setTypeVariable(TargetType()) },
        build {}
    )
    return "OK"
}




fun <T> selectBuildee(vararg values: Buildee<T>): Buildee<T> = values.first()

class TargetType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
