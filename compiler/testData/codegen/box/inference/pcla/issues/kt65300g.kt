// ISSUE: KT-65300

fun box(): String {
    build {
        fun(): Buildee<TargetType> = this
    }
    return "OK"
}




class TargetType

class Buildee<TV>

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
