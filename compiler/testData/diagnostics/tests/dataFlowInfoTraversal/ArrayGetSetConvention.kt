trait G {
    fun get(x: Int, y: Int): Int = x + y
    fun set(x: Int, y: Int, value: Int) {}
}

fun foo1(a: Int?, b: G) {
    b[a!!, a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>] = <!DEBUG_INFO_AUTOCAST!>a<!>
    <!DEBUG_INFO_AUTOCAST!>a<!> : Int
}

fun foo2(a: Int?, b: G) {
    b[0, a!!] = <!DEBUG_INFO_AUTOCAST!>a<!>
    <!DEBUG_INFO_AUTOCAST!>a<!> : Int
}

fun foo3(a: Int?, b: G) {
    val r = b[a!!, <!DEBUG_INFO_AUTOCAST!>a<!>]
    <!DEBUG_INFO_AUTOCAST!>a<!> : Int
    r : Int
}

fun foo4(a: Int?, b: G) {
    val r = b[0, a!!]
    <!DEBUG_INFO_AUTOCAST!>a<!> : Int
    r : Int
}
