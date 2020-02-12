// IGNORE_BACKEND: JVM_IR
// KT-36641 TODO Generate IINC instruction for prefix increment in JVM_IR

fun main(args: Array<String>) {
    var i = 10
    ++i
    ++(l@ i)
}

// 2 IINC