// IGNORE_BACKEND_FIR: JVM_IR
class MyList<T>() {
    var value: T? = null

    operator fun get(index: Int): T = value!!

    operator fun set(index: Int, value: T) { this.value = value }
}

fun box(): String {
    val list = MyList<Int>()
    list[17] = 1
    list[17] = list[18]++
    return "OK"
}
