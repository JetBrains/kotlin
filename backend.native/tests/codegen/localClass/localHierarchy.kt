fun foo(s: String): String {
    open class Local {
        fun f() = s
    }

    open class Derived: Local() {
        fun g() = f()
    }

    return Derived().g()
}

fun main(args: Array<String>) {
    println(foo("OK"))
}