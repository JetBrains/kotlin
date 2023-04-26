// !LANGUAGE: +ReportErrorsOnRecursiveTypeInsidePlusAssignment
// WITH_STDLIB
// FIR: KT-51648

object DelegateTest {
    var result = ""
    val f by lazy {
        result += <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT_ERROR!>f<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>toString<!>() // Compiler crash
        "hello"
    }
}

object DelegateTest2 {
    var result = ""
    val f by lazy {
        result += <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT_ERROR!>f<!>
        "hello"
    }

    var intResult = 0
    val i1 by lazy {
        intResult <!OVERLOAD_RESOLUTION_AMBIGUITY!>+=<!> <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT_ERROR!>i1<!>
        0
    }
    val i2 by lazy {
        intResult <!OVERLOAD_RESOLUTION_AMBIGUITY!>-=<!> <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT_ERROR!>i2<!>
        0
    }
    val i3 by lazy {
        intResult <!OVERLOAD_RESOLUTION_AMBIGUITY!>*=<!> <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT_ERROR!>i3<!>
        0
    }
    val i4 by lazy {
        intResult <!OVERLOAD_RESOLUTION_AMBIGUITY!>/=<!> <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT_ERROR!>i4<!>
        0
    }
    val i5 by lazy {
        intResult <!OVERLOAD_RESOLUTION_AMBIGUITY!>%=<!> <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT_ERROR!>i5<!>
        0
    }
}
