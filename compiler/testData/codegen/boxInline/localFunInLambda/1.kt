import test.*

fun test1(d: Data): Int {
    val input = Input(d)
    var result = 10
    with(input) {
        fun localFun() {
            result = input.d.value
        }
        localFun()
    }
    return result
}


fun box(): String {
    val result = test1(Data(11))
    if (result != 11) return "test1: ${result}"

    return "OK"
}
