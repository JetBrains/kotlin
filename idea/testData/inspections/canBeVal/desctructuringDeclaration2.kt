fun foo(p: Int) {
    var (v1, v2) = getPair()
    print(v1)
    v2 = ""
}

fun getPair(): Pair<Int, String> = 1 to ""