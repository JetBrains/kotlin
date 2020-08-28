// MODE: all

fun main(args: Array<String>) {
    val (a: String, b: String, c: String) = x()
}

fun x() :Triple<String, String,String> {
    return Triple("A", "B", "C")
}