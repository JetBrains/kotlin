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
    class NestedClass(val d: Double) {
        companion object {
            const val CONST = 2.0
        }
    }

    companion object {
        val STR = "str"
    }
}
