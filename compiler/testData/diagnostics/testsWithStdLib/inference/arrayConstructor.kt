// !CHECK_TYPE

package b

fun bar() {
    val a1 = Array(1, {i: Int -> i})
    val a2 = Array(1, {i: Int -> "$i"})
    val a3 = Array(1, {it})

    checkSubtype<Array<Int>>(a1)
    checkSubtype<Array<String>>(a2)
    checkSubtype<Array<Int>>(a3)
}

