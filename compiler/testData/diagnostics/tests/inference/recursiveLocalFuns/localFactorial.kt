// See KT-6271
fun foo() {
    fun fact(n: Int) = {
        if (n > 0) {
            <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!><!DEBUG_INFO_MISSING_UNRESOLVED!>fact<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>n<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>-<!> 1)<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>*<!> n
        }
        else {
            1
        }
    }
}
