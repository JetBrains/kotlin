fun <T> eval(fn: () -> T) = fn()

fun box(): String {
    var s = ""
    var foo = "K"

    fun foo(x: String, y: Int) {
        s += x
    }

    fun test() {
        fun foo(x: String) {
            s += x
        }

        eval {
            foo("O")
            foo(foo, 1)
        }
    }

    test()

    return s
}