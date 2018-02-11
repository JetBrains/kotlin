// !WITH_NEW_INFERENCE
// See KT-6271
fun foo() {
    fun fact(n: Int) = {
        if (n > 0) {
            <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!NI;DEBUG_INFO_MISSING_UNRESOLVED!>fact<!>(<!NI;DEBUG_INFO_MISSING_UNRESOLVED!>n<!> <!NI;DEBUG_INFO_MISSING_UNRESOLVED!>-<!> 1)<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>*<!> n
        }
        else {
            1
        }
    }
}
