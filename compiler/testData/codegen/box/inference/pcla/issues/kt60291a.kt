// ISSUE: KT-60291
// WITH_STDLIB

// IGNORE_BACKEND: ANDROID

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
