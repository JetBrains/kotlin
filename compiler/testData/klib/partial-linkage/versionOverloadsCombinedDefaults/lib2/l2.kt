fun test1(): String =
    if (fooMiddleInsertFun(0) == 13) "OK" else "FAIL1"

fun test2(): String =
    if (fooCtor(0).value == 13) "OK" else "FAIL2"

fun test3(): String =
    if (fooRelabel(0) == 1) "OK" else "FAIL3"

fun test4(): String =
    if (fooDefaultChain() == 4) "OK" else "FAIL4"

fun test5(): String =
    if (fooComparableOrder() == "a=1,b=19,c=102,d=110") "OK" else "FAIL5"

fun test6(): String {
    val d1 = fooData(a = 1, b = "B")
    val d2 = d1.copy(b = "X")
    return if (d2.a == 1 && d2.b == "X") "OK" else "FAIL6"
}
