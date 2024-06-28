// ISSUE: KT-52838
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = build {
        this as DerivedBuildee<*>
        getTypeVariable()
        <!RECEIVER_TYPE_MISMATCH("CapturedType(*); CapturedType(*)"), RECEIVER_TYPE_MISMATCH("CapturedType(*); CapturedType(*)")!>getTypeVariable<!>()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<*>>(<!TYPE_MISMATCH("Buildee<Any?>; Buildee<*>")!>buildee<!>)
}




open class Buildee<TV> {
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

class DerivedBuildee<TA>: Buildee<TA>()

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return DerivedBuildee<PTV>().apply(instructions)
}
