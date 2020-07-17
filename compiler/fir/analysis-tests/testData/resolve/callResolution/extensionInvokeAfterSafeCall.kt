interface A

fun test_1(a: A?, convert: A.() -> String) {
    val s = a?.convert()
}

fun test_2(a: A, convert: A.() -> String) {
    val s = a.convert()
}
