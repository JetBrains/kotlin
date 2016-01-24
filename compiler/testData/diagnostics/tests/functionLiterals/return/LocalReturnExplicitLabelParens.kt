// !CHECK_TYPE

fun test() {
    val x = run(f@{return@f 1})
    checkSubtype<Int>(x)
}


fun test1() {
    val x = run(l@{return@l 1})
    checkSubtype<Int>(x)
}
