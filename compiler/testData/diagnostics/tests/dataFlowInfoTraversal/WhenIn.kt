fun foo(x: Int, list: List<Int>?) {
    when (x) {
        in list!! -> <!DEBUG_INFO_AUTOCAST!>list<!> : List<Int>
        else -> {}
    }
}

fun whenWithoutSubject(x: Int, list: List<Int>?) {
    when {
        x in list!! -> <!DEBUG_INFO_AUTOCAST!>list<!> : List<Int>
        else -> {}
    }
}
