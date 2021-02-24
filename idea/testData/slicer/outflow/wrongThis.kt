// FLOW: OUT
// WITH_RUNTIME

class C {
    fun String.extensionFun(): Any {
        with("A") {
            println(this.length)
            println(this@extensionFun.length)
        }
        return this@C
    }

    fun foo() {
        val x = <caret>"".extensionFun()
    }
}
