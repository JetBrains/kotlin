package pack

annotation class Anno(val value: Int)

open class SuperClass {
    companion object {
        const val CONST = 1
    }
}

class TopLevelClass <expr>@Anno(CONST)</expr> constructor(val i: Int) : SuperClass() {
    val CONST = "str"

    companion object {
        val STR = "str"
    }
}
