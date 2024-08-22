fun main() {
    val case1 = "1" + "2"
    val case2 = "1" + "2" + "3"
    val case3 = ("1" + "2") + "3"
    val case4 = "1" + ("2" + "3")
    val case5 = "1" + case1
    val case6 = "1" + "2" + case1
    val case6a = ("1" + "2") + case1
    val case7 = case1 + "1" + "2"
    val case7a = case1 + ("1" + "2")
    val case8 = "1" + case1 + "2"
    val case9 = 1 + 2 + 3
    val case10 = case1 + case1 + case1
    val case11 = "1" + "2" + "3" + "4" + "5" + "6"
    val case12 = "1" <!UNRESOLVED_REFERENCE!>-<!> "2"
    val case13 = <!EXPRESSION_EXPECTED!>while(true) {}<!> + "2" + "3"
    val case14 = "1" + "2" + f("1" + "2")
}

fun f(item: String): String = item
