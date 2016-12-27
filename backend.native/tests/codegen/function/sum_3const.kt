fun sum3():Int = sum(1, 2, 33)
fun sum(a:Int, b:Int, c:Int): Int = a + b + c

fun main(args: Array<String>) {
    if (sum3() != 36) throw Error()
}