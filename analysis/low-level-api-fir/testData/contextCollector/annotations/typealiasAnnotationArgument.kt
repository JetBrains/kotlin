package pack

annotation class Anno(val value: Int)

open class SuperClass {
    companion object {
        const val CONST = 1
    }
}

class TopLevelClass(val i: Int) : SuperClass() {
    val CONST = "str"

    @Anno(<expr>CONST</expr>)
    typealias Alias = TopLevelClass

    companion object {
        val STR = "str"
    }
}
