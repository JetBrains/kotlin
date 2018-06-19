class Test {
    companion object {
        fun globalFun() = 1
    }

    fun test() {
        <caret>Companion.globalFun()
    }
}

fun globalFun() = 2