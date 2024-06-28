// ISSUE: KT-54767
// FIR_IDENTICAL
// CHECK_TYPE_WITH_EXACT
// WITH_STDLIB

class Klass {
    val buildee by lazy {
        build {
            setTypeVariable(TargetType())
        }
    }
}

fun test() {
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(Klass().buildee)
}




class TargetType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
