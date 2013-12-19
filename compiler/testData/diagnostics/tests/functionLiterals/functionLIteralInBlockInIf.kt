// !CHECK_TYPE
fun test() {
    val a = if (true) {
        val x = 1
        ({ x })
    } else {
        { 2 }
    }
    a checkType { it : _<() -> Int> }
}