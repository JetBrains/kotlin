// !CHECK_TYPE
fun test() {
    val a = if (true) {
        val x = 1
        ({ x })
    } else {
        { 2 }
    }
    a checkType {  <!UNRESOLVED_REFERENCE!>_<!><() -> Int>() }
}