inline fun test(s: ()->Int): Int {
    var i = 0;
    i = s()
    try {
        i = i + 10
    } finally {
        return i
    }
}

fun box() : String {
    var p: Int = 1
    test {
        try {
            p = 1
            return "OK" //finally from inline fun doen't split this try
        } catch(e: Exception) {
            p = -1;
            p
        } finally {
            p++
        }

    }
    return "fail"
}

// 10 TRYCATCHBLOCK