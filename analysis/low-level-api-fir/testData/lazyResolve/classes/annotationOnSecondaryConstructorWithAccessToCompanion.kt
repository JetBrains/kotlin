annotation class Anno(val value: Int)

open class SuperClass {
    companion object {
        const val CONST = "str"
    }
}

class TopLevelClass : SuperClass {
    @Anno(CONST)
    <caret>constructor(val i: Int) : super()

    companion object {
        const val CONST = 1
    }
}
