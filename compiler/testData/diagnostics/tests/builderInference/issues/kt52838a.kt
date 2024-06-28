// ISSUE: KT-52838
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = build {
        this as DerivedBuildee<*, *>
        getTypeVariableA()
        <!RECEIVER_TYPE_MISMATCH("CapturedType(*); CapturedType(*)"), RECEIVER_TYPE_MISMATCH("CapturedType(*); CapturedType(*)"), RECEIVER_TYPE_MISMATCH("CapturedType(*); CapturedType(*)"), RECEIVER_TYPE_MISMATCH("CapturedType(*); CapturedType(*)")!>getTypeVariableB<!>()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<*, *>>(<!TYPE_MISMATCH("Buildee<Any?, Any?>; Buildee<*, *>")!>buildee<!>)
}




open class Buildee<TVA, TVB> {
    fun getTypeVariableA(): TVA = storageA
    fun getTypeVariableB(): TVB = storageB
    private var storageA: TVA = null!!
    private var storageB: TVB = null!!
}

class DerivedBuildee<TAA, TAB>: Buildee<TAA, TAB>()

fun <PTVA, PTVB> build(instructions: Buildee<PTVA, PTVB>.() -> Unit): Buildee<PTVA, PTVB> {
    return DerivedBuildee<PTVA, PTVB>().apply(instructions)
}
