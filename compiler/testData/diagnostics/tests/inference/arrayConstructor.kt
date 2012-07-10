package b

fun bar() {
    val a1 = Array(1, {(i: Int) -> i})
    val a2 = Array(1, {(i: Int) -> "$i"})
    val a3 = Array(1, {it})

    a1 : Array<Int>
    a2 : Array<String>
    a3 : Array<Int>
}

