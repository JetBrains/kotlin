// ISSUE: KT-52838
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        this as DerivedBuildee<*, *>
        getTypeVariableA()
        getTypeVariableB()
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<*, *>>(<!ARGUMENT_TYPE_MISMATCH!>buildee<!>)
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
