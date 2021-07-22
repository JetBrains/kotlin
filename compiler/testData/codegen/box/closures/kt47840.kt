fun box(): String {
    var t = ""
    fun foo(x: String) {
        fun bar() {
            fun a() {
                foo("")
                bar()
            }
            t = x
        }

        bar()
    }
    foo("OK")
    return t
}
