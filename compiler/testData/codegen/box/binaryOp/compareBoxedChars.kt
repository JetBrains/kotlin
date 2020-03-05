fun <T> id(x: T) = x

fun box(): String {
    if (id('a') > id('b')) return "fail 1"
    if (id('a') >= id('b')) return "fail 2"
    if (id('b') < id('a')) return "fail 3"
    if (id('b') <= id('a')) return "fail 4"

    val x = id('a').compareTo('b')
    if (x != -1) return "fail 5"

    val y = id('b').compareTo('a')
    if (y != 1) return "fail 6"

    return "OK"
}
