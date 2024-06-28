private var privateVar = 12

internal inline fun customSetVar(value: Int) {
    privateVar = value
}

internal inline fun customGetVar(): Int = privateVar

fun box(): String {
    var result = 0
    result += customGetVar()
    customSetVar(3)
    result += customGetVar()
    if (result != 15) return result.toString()
    return "OK"
}
