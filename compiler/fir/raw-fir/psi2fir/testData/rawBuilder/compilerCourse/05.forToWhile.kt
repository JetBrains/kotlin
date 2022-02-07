fun takeInt(x: Int) {}

fun test(list: List<Int>) {
    println("start")
    for (x in list) {
        takeInt(x)
    }
    println("end")
}
