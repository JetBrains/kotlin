// WITH_RUNTIME

class Host(var value: String) {
    operator fun get(i: Int, j: Int, k: Int) = value

    operator fun set(i: Int, j: Int, k: Int, newValue: String) {
        value = newValue
    }
}

fun box(): String {
    var x = Host("")
    run {
        x[0, 0, 0] += "O"
        x[0, 0, 0] += "K"
    }
    return x.value
}