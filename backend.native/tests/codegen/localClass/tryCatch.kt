private fun foo() {
    val local =
            object {
                fun bar() {
                    try {
                    } catch (t: Throwable) {
                        println(t)
                    }
                }
            }
    local.bar()
}

fun main(args: Array<String>) {
    foo()
}