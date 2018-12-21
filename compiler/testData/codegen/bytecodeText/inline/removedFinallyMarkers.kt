// IGNORE_BACKEND: JVM_IR
inline fun test(s: ()->Int){
    var i = 0;
    try {
        i = s()
        i = i + 10
    } finally {
        //finallyStart
        i
        //finallyEnd
        //and same markers in default catch handler
    }
}

fun box() : String {
    var p: Int = 1
    test {
        try {
            p = 1
            return "OK"
        } catch(e: Exception) {
            p = -1;
            p
        } finally {
            p++
        }

    }
    return "fail"
}

// 2 InlineMarker.finallyStart
// 2 InlineMarker.finallyEnd
// 4 InlineMarker