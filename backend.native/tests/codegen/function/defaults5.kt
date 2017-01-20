class Test(val x: Int) {
    fun foo(y: Int = x) {
        println(y)
    }
}

fun Test.bar(y: Int = x) {
    println(y)
}

fun main(args: Array<String>) {
    Test(5).foo()
    Test(6).bar()
}