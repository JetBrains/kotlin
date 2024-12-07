// ISSUE: KT-54400

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        typeVariableMutableProperty = 42
    }
    return "OK"
}




class Buildee<TV> {
    var typeVariableMutableProperty: TV
        get() = storage
        set(value) { storage = value }
    private var storage: TV = 42 as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
