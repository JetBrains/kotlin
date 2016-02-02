interface A {

    private val prop: String
        get() = "1"

    private fun foo() {

    }

    private fun defaultFun(p: String = "OK") {

    }
}

// 1 foo\(
// 1 getProp
// 1 defaultFun\$
