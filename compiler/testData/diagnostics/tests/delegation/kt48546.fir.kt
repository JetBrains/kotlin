// WITH_STDLIB

object DelegateTest {
    var result = ""
    val f by lazy {
        result += f.toString() // Compiler crash
        "hello"
    }
}