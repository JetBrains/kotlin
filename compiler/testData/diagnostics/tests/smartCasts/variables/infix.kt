// See KT-774
fun box() : Int {
    var a : Any = 1
    var d = 1

    if (a is Int) {
        return <!DEBUG_INFO_SMARTCAST!>a<!> + d
    } else {
        return 2
    }
} 