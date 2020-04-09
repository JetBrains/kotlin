// FLOW: OUT

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

inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    return receiver.block()
}
