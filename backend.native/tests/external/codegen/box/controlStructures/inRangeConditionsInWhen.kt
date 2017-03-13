operator fun Int.contains(i : Int) = true

fun box(): String {
    when (1) {
        in 2 -> return "OK"
        else -> return "fail"
    }
}
