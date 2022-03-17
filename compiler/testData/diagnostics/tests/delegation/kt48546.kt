// WITH_STDLIB

object DelegateTest {
    var result = ""
    val f by lazy {
        result += <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>f<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>toString<!>() // Compiler crash
        "hello"
    }
}