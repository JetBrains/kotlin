// ISSUE: KT-63840
// WITH_STDLIB

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

// IGNORE_BACKEND_K2: JVM_IR, WASM
// REASON: run-time failure (java.lang.ArrayStoreException: TargetType @ Kt63840aKt$box$1.invoke)

fun box(): String {
    build {
        select(
            replaceTypeVariable(TargetType()),
            DifferentType()
        )
    }
    return "OK"
}




fun <T> select(vararg values: T): T = values.first()

class TargetType
class DifferentType

class Buildee<TV> {
    fun replaceTypeVariable(value: TV): TV { val temp = storage; storage = value; return temp }
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
