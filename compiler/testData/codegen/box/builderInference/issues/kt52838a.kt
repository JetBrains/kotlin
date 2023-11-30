// ISSUE: KT-52838

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        this as DerivedBuildee<*, *>
        getTypeVariableA()
        getTypeVariableB()
    }
    return "OK"
}




open class Buildee<TVA, TVB> {
    fun getTypeVariableA(): TVA = storageA
    fun getTypeVariableB(): TVB = storageB
    private var storageA: TVA = Any() as TVA
    private var storageB: TVB = Any() as TVB
}

class DerivedBuildee<TAA, TAB>: Buildee<TAA, TAB>()

fun <PTVA, PTVB> build(instructions: Buildee<PTVA, PTVB>.() -> Unit): Buildee<PTVA, PTVB> {
    return DerivedBuildee<PTVA, PTVB>().apply(instructions)
}
