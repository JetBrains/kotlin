// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ReportErrorsOnRecursiveTypeInsidePlusAssignment
// WITH_STDLIB
// FIR: KT-51648

object DelegateTest {
    var result = ""
    val f by lazy {
        result += <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT_WARNING!>f<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>toString<!>() // Compiler crash
        "hello"
    }
}

object DelegateTest2 {
    var result = ""
    val f by lazy {
        result += <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT_WARNING!>f<!>
        "hello"
    }
}
