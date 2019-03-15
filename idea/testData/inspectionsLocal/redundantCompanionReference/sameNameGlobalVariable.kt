class Test {
    companion object {
        val globalVar = 1
    }

    fun test() {
        <caret>Companion.globalVar
    }
}

val globalVar = 2