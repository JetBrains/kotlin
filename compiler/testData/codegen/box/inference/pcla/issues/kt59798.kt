// ISSUE: KT-59798

// IGNORE_BACKEND: ANDROID

fun box(): String {
    build {
        setTypeVariable(TargetType())
        getTypeVariable().let {}
    }
    return "OK"
}




class TargetType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
