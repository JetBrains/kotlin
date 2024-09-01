// ISSUE: KT-60291
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = when ("") {
        "true" -> build { setTypeVariable(TargetType()) }
        "false" -> build {}
        else -> Buildee()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




class TargetType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}