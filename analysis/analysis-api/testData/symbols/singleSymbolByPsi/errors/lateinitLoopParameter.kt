// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE

class A

fun f() {
    for (lateinit somet<caret>hing: A in listOf<A>()) {}
}