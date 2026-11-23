class C(flag: Boolean) {
    val member by if (flag) {
        @Suppress("") val n = 1
        lazy { n }
    } else {
        lazy { 0 }
    }
}

val flag = true

val topLevel by if (flag) {
    @Suppress("") val m = 2
    lazy { m }
} else {
    lazy { 0 }
}
