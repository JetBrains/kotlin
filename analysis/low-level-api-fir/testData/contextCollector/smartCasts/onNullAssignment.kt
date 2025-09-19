// WITH_STDLIB

fun main() {
    var str: String? = null
    str = "not null"
    println(str)
    <expr>str</expr> = null
    println(str)
}