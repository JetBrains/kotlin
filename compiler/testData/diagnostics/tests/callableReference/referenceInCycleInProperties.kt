// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
abstract class Parser {
    open fun parseString(x: String): List<Int> = null!!

    fun parse(name: String): Int = null!!
    fun parse(name: String, content: String): Int = null!!
}

class Some(strings: List<String>) {
    val parser = object : Parser() {
        override fun parseString(x: String) = listOfInt
    }
    private val listOfString = strings
    private val listOfInt: List<Int> = listOfString.map(parser::parse)
}
