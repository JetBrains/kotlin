fun foo(x: Int, list: List<Int>?) {
    when (x) {
        in list!! -> <!DEBUG_INFO_SMARTCAST!>list<!> : List<Int>
        else -> {}
    }
}

fun whenWithoutSubject(x: Int, list: List<Int>?) {
    when {
        x in list!! -> <!DEBUG_INFO_SMARTCAST!>list<!> : List<Int>
        else -> {}
    }
}
