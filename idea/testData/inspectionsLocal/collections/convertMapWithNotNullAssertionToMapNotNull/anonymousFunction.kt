// WITH_RUNTIME

fun test(list: List<Int?>) {
    list.<caret>map(fun(it: Int?): Int {
        foo()
        return it!!
    })
}

fun foo(): Int = 1