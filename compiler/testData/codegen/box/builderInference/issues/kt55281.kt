// ISSUE: KT-55281

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K2: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        this as DerivedBuildee<*>
        consumeNullableAny(getTypeVariable())
    }
    return "OK"
}




class TargetType

fun consumeNullableAny(value: Any?) {}

open class Buildee<TV> {
    fun getTypeVariable(): TV = storage
    private var storage: TV = null as TV
}

class DerivedBuildee<TA>: Buildee<TA>()

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return DerivedBuildee<PTV>().apply(instructions)
}
