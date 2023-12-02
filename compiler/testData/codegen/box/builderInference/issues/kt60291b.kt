// ISSUE: KT-60291

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

// IGNORE_BACKEND_K2: NATIVE, WASM, JS_IR, JS_IR_ES6
// REASON: compile-time failure (org.jetbrains.kotlin.backend.common.linkage.issues.IrDisallowedErrorNode: Class found but error nodes are not allowed)

fun box(): String {
    if (true)
        build { setTypeVariable(TargetType()) }
    else
        build {}
    return "OK"
}




class TargetType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
