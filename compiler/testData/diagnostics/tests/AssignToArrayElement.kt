fun getArray(): Array<Int> = throw Exception()
fun getList(): MutableList<Int> = throw Exception()
fun fn() {
    getArray()[1] = 2
    getList()[1] = 2
    getArray()[1]++
    getList()[1]++
    getArray()[1] += 2
    getList()[1] += 2
}