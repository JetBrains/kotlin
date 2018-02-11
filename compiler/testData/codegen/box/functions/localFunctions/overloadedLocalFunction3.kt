fun box(): String {
    var s = ""
    var foo = "O"

    fun foo(x: String, z: Int) {
        s += x
    }

    run {
        fun foo(x: String) {
            s += x
        }

        {
            foo(foo, 1)
            foo("K")
        } ()
    }

    return s
}