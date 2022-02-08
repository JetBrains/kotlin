fun kt49182(x: Boolean): String {
    return if (x) {
        return "O"
    } else "K"
}

var flag = true

fun exit(): Nothing = null!!

fun box(): String {
    if (kt49182(true) + kt49182(false) != "OK")
        return "Fail test2"

    val a: String
    if (flag) {
        a = "OK"
    }
    else {
        exit()
    }
    return a
}
