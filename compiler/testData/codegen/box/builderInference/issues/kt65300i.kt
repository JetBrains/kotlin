// ISSUE: KT-65300

fun box(): String {
    build {
        class LocalClass {
            var typeInfoSourcePropertyWithBackingField: Buildee<TargetType> = this@build
        }
    }
    return "OK"
}




class TargetType

class Buildee<TV>

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
