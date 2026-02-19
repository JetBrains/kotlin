// WITH_STDLIB

fun main() {
    var str: String? = null
    str = "not null"
    println(str)
    <expr>str = null</expr>
    println(str)
}