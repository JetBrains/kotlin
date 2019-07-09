// IGNORE_BACKEND: JVM_IR
fun main(args: Array<String>) {
    var i = 10
    ++i
    ++(l@ i)
}

// 2 IINC