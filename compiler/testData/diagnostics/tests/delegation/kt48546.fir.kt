// WITH_STDLIB

object DelegateTest {
    var result = ""
    val f by lazy {
        result += <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>f<!>.toString() // Compiler crash
        "hello"
    }
}
