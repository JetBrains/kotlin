class Test {
    var x: Int = 42
}

fun foo(): Test {
    println(42)
    return Test()
}

fun main(args: Array<String>) {
    foo()::x
}