// FIR_IDENTICAL
// WITH_STDLIB

fun test1() {
    var list = ArrayList<Int>()
    list -= 2
}

fun test2() {
    var set = HashMap<Int, Int>()
    set += 2 to 2
}

fun test3() {
    var set = HashSet<Int>()
    set -= 2
}

fun test4() {
    var list = mutableListOf(1)
    list += 2
}

fun test5() {
    var map = mutableMapOf(1 to 1)
    map += 2 to 2
}

fun test6() {
    var set = mutableSetOf(1)
    set += 2
}