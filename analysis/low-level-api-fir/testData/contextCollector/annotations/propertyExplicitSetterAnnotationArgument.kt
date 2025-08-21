package pack

annotation class Anno(val value: Int)

open class SuperClass {
    companion object {
        const val CONST = 1
    }
}

class TopLevelClass(val i: Int) : SuperClass() {
    val CONST = "str"

    var <T> T.foo get() = 0
        @Anno(<expr>CONST</expr>)
        set(value) {}

    companion object {
        val STR = "str"
    }
}
