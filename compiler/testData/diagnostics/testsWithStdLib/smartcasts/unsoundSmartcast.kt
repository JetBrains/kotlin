// SKIP_TXT
// ISSUE: KT-30507

fun test1() {
    var x: MutableList<Int>? = mutableListOf(1)
    x!!
    <!DEBUG_INFO_SMARTCAST!>x<!>[if (true) { x = null; 0 } else 0] += <!DEBUG_INFO_SMARTCAST!>x<!>[0]
    <!DEBUG_INFO_SMARTCAST!>x<!>[0].inv()
}

fun test2() {
    var x: MutableList<Int>? = mutableListOf(1)
    x!!
    <!DEBUG_INFO_SMARTCAST!>x<!>[if (true) { x = null; 0 } else 0] = 11
    <!UNSAFE_CALL!>x[0]<!>.inv()
}

fun test3() {
    var x: MutableList<Int>? = mutableListOf(1)
    x!!
    <!DEBUG_INFO_SMARTCAST!>x<!>[0] = if (true) { x = null; 0 } else 0
    <!UNSAFE_CALL!>x[0]<!>.inv()
}

operator fun MutableList<Int>.get(i1: Int, i2: Int) {}
operator fun MutableList<Int>.set(i1: Int, i2: Int, el: Int) = 10

fun test4() {
    var x: MutableList<Int>? = mutableListOf(1)
    x!!
    val y = <!DEBUG_INFO_SMARTCAST!>x<!>[if (true) { x = null; 0 } else 0, <!DEBUG_INFO_SMARTCAST!>x<!>[0]]
}

operator fun Int.invoke(y: Int, z: Int) {}

fun test5() {
    var x: Int? = 10
    x!!
    <!DEBUG_INFO_SMARTCAST!>x<!>(if (true) { x = null; 0 } else 0, <!DEBUG_INFO_SMARTCAST!>x<!>)
    <!DEBUG_INFO_SMARTCAST!>x<!>.inv()
}
