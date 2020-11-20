// IGNORE_BACKEND_FIR: JVM_IR
fun main(args: Array<String>) {
    var i = 10
    ++i
    ++(l@ i)
}

// 2 IINC