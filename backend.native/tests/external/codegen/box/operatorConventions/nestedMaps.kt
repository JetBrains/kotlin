object Map1 {
    operator fun get(i: Int, j: Int) = Map2
}

object Map2 {
    operator fun get(i: Int, j: Int) = 0
    operator fun set(i: Int, j: Int, newValue: Int) {}
}

fun box(): String {
    Map1[0, 0][0, 0]++
    return "OK"
}