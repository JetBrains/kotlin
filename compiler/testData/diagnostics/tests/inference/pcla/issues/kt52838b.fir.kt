// ISSUE: KT-52838
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = build {
        this as DerivedBuildee<*>
        getTypeVariable()
        getTypeVariable()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<*>>(<!ARGUMENT_TYPE_MISMATCH!>buildee<!>)
}




open class Buildee<TV> {
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

class DerivedBuildee<TA>: Buildee<TA>()

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return DerivedBuildee<PTV>().apply(instructions)
}
