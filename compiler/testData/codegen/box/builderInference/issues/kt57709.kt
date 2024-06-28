// ISSUE: KT-57709
// WITH_STDLIB

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JVM
// REASON: red code (see corresponding diagnostic test)

var result = ""

@JvmName("consumeAnyBuildeeReceiver")
fun Buildee<Any>.consumeBuildeeReceiver() {
    result += "Any/"
}
@JvmName("consumeDifferentTypeBuildeeReceiver")
fun Buildee<DifferentType>.consumeBuildeeReceiver() {
    result += "DifferentType/"
}

fun box(): String {
    build<Any> {
        setTypeVariable(Any())
        consumeBuildeeReceiver()
    }
    build {
        setTypeVariable(Any())
        consumeBuildeeReceiver()
    }
    return if (result == "Any/Any/") "OK" else "FAIL: $result"
}




class DifferentType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = Any() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
