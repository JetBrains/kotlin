fun main(args: Array<String>) {
    println("false".toBoolean())
    println("true".toBoolean())
    println("-1".toByte())
    println("a".toByte(16))
    println("aa".toShort(16))
    println("11110".toInt(2))
    println("ffffffff".toLong(16))
    try {
        val x = "ffffffff".toLong(10)
    } catch (ne: NumberFormatException) {
        println("bad format")
    }
    println("0.5".toFloat())
    println("2.39".toDouble())
}