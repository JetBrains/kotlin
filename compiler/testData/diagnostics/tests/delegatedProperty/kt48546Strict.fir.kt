// !LANGUAGE: +ReportErrorsOnRecursiveTypeInsidePlusAssignment
// WITH_STDLIB
// FIR: KT-51648

object DelegateTest {
    var result = ""
    val f by lazy {
        result += <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: cycle")!>f<!>.toString() // Compiler crash
        "hello"
    }
}

object DelegateTest2 {
    var result = ""
    val f by lazy {
        result += <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: cycle")!>f<!>
        "hello"
    }
}