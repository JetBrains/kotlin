// IGNORE_BACKEND: JS
fun test(): Boolean {
    var flagOuter = false
    var flagInner = false
    for (i in 0..5) {
        when (i) {
            1 -> Unit
            2 -> Unit
            3 -> {
                for (j in 0..5) {
                    when (j) {
                        1 -> Unit
                        2 -> {
                            flagInner = true
                            break
                        }
                        else -> flagInner = false
                    }
                }
            }
            4 -> {
                flagOuter = true
                break
            }
            else -> flagOuter = false
        }
    }

    return flagOuter and flagInner
}

fun box(): String {
    val flag = test()
    return if (flag) "OK" else "fail1"
}