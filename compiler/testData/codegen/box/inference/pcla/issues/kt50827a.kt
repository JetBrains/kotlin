// ISSUE: KT-50827

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    val box = ClassWithBoundedTypeParameter(
        build {
            setTypeVariable(TargetType())
        }
    )
    consumeTargetTypeBuildee(box.buildee)
    return "OK"
}




class TargetType

fun consumeTargetTypeBuildee(value: Buildee<TargetType>) {}

class ClassWithBoundedTypeParameter<T: Any>(val buildee: Buildee<T>)

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
