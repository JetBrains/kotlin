package a

fun test1() {
    bar(
        11,
        todo(),//comment1
        ""//comment2
    )
}

fun test2() {
    bar(11, todo()/*comment1*/, ""/*comment2*/)
}
fun test3() {
    bar(11, l@(todo()/*comment*/), "")
}

fun todo(): Nothing = throw Exception()

fun bar(i: Int, s: String, a: Any) {}


