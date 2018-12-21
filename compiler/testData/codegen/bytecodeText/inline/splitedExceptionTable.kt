// IGNORE_BACKEND: JVM_IR
inline fun test(s: ()->Int){
    var i = 0;
    try {
        i = s()
        i = i + 10
    } finally {
        i
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
/* 8 + 1 see notSplitedExceptionTable.kt*/
// 9 TRYCATCHBLOCK
