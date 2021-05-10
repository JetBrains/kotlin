// FIR_COMPARISON
interface Interface {
    fun funA()
}

expect class SClass : Interface { // there is no error highlighting about unimplemented members, see KT-25044
    override fu<caret>
}
// ELEMENT_TEXT: "override fun funA() {...}"