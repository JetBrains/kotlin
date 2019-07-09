fun foo() {
    for (i in 1..10) {
        println(i)
    }
}

fun bar(list: List<String>) {
    for (element in list.subList(0, 10)) {
        println(element)
    }
    for (element in list.subList(10, 20)) println(element)
}

data class Some(val x: Int, val y: Int)

fun baz(set: Set<Some>) {
    for ((x, y) in set) {
        println("x = $x y = $y")
    }
}
