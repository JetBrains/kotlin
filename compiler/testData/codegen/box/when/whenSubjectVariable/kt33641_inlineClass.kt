// WITH_STDLIB

fun box(): String {
    val x = Result.success(42)
    var y = 0
    when (x) {
        Result.success(42) -> {
            y++
        }
        else -> {
            y -= 10
        }
    }
    when (val z = Result.success(42)) {
        Result.success(42) -> {
            y++
        }
        else -> {
            y -= 100
        }
    }
    return if (y == 2) "OK" else "Fail $y"
}
