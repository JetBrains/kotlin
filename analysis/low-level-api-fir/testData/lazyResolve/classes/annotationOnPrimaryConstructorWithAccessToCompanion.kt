annotation class Anno(val value: Int)

open class SuperClass {
    companion object {
        const val CONST = "str"
    }
}

class TopLevelClass @Anno(CONST) <caret>constructor(val i: Int) : SuperClass() {
    companion object {
        const val CONST = 1
    }
}
