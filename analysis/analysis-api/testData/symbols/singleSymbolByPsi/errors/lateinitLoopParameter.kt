// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// PSI/NON-PSI DIFF: `lateinit` is not propagated to the FIR status of loop parameters, see changes in KT-76578

class A

fun f() {
    for (lateinit somet<caret>hing: A in listOf<A>()) {}
}