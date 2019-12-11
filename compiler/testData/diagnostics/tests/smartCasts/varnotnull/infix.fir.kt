// See KT-774
fun box() : Int {
    var a : Int? = 1
    var d = 1

    if (a == null) {
        return 2
    } else {
        return a + d
    }
} 