package w_range

fun box() : String {
    var i = 0
    when (i) {
        1 -> i--
        else -> { i = 2 }
    }
    return if (i == 2) "OK" else i.toString()
}
