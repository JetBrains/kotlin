// ISSUE: KT-50827
// CHECK_TYPE_WITH_EXACT

fun test() {
    val box = ClassWithBoundedTypeParameter(
        build {
            setTypeVariable(TargetType())
        }
    )
    val buildee = box.buildee
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(<!ARGUMENT_TYPE_MISMATCH("Buildee<TargetType>; Buildee<kotlin.Any>")!>buildee<!>)
}




class TargetType

class ClassWithBoundedTypeParameter<T: Any>(val buildee: Buildee<T>)

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
