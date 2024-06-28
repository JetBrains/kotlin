// ISSUE: KT-54767
// WITH_STDLIB

class Klass {
    val buildee by lazy {
        build {
            setTypeVariable(TargetType())
        }
    }
}

fun box(): String {
    Klass().buildee
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
