import java.util.ArrayList

fun sparse(x: Int): Int {
    return when ((x % 4) * 100) {
        100 -> 1
        200 -> 2
        300 -> 3
        else -> 4
    }
}

fun box(): String {
    var result = (0..3).map(::sparse).makeString()

    if (result != "4, 1, 2, 3") return "sparse:" + result
    return "OK"
}

