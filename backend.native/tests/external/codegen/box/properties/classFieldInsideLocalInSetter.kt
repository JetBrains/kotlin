class My {
    var my: String = "U"
        get() = { field }()
        set(arg) {
            class Local {
                fun foo() {
                    field = arg + "K"
                }
            }
            Local().foo()
        }
}

fun box(): String {
    val m = My()
    m.my = "O"
    return m.my
}