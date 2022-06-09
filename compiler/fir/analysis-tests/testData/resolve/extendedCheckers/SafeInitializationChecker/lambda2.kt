class A {
    val q: String = ""
    val s: String
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val a: String =
        run {
            this@A.s = ""
            a
        }<!>

    val b: String

    init {
        let {
            b = s
        }
    }
}