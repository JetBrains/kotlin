// WITH_STDLIB

object DelegateTest {
    var result = ""
    val f by lazy {
        result += <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_WARNING!>f<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>toString<!>() // Compiler crash
        "hello"
    }
}