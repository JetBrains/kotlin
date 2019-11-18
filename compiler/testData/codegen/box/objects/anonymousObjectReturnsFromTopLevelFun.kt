// IGNORE_BACKEND_FIR: JVM_IR
interface IFoo {
    fun foo(): String
}

interface IBar

private fun createAnonObject() =
        object : IFoo, IBar {
            override fun foo() = "foo"
            fun qux(): String = "qux"
        }

fun useAnonObject() {
    createAnonObject().foo()
    createAnonObject().qux()
}

fun box(): String {
    if (createAnonObject().foo() != "foo") return "fail 1"
    if (createAnonObject().qux() != "qux") return "fail 2"
    return "OK"
}