fun Array<Array<Array<Array<Array<Array<Array<Array<Array<Array<Array<Array<Array<Array<Array<LongArray>>>>>>>>>>>>>>>.dimensions() = "OK"

fun box(): String =
    arrayOf(arrayOf(arrayOf(arrayOf(arrayOf(arrayOf(arrayOf(arrayOf(arrayOf(arrayOf(arrayOf(arrayOf(arrayOf(arrayOf(arrayOf(
        longArrayOf(42L)
    ))))))))))))))).dimensions()
