// WITH_RUNTIME

fun test1() {
    var list = ArrayList<Int>()
    <!ASSIGN_OPERATOR_AMBIGUITY!>list -= 2<!>
}

fun test2() {
    var set = HashMap<Int, Int>()
    <!ASSIGN_OPERATOR_AMBIGUITY!>set += 2 to 2<!>
}

fun test3() {
    var set = HashSet<Int>()
    <!ASSIGN_OPERATOR_AMBIGUITY!>set -= 2<!>
}

fun test4() {
    var list = mutableListOf(1)
    <!ASSIGN_OPERATOR_AMBIGUITY!>list += 2<!>
}

fun test5() {
    var map = mutableMapOf(1 to 1)
    <!ASSIGN_OPERATOR_AMBIGUITY!>map += 2 to 2<!>
}

fun test6() {
    var set = mutableSetOf(1)
    <!ASSIGN_OPERATOR_AMBIGUITY!>set += 2<!>
}