// PROBLEM: none

import MVE.Used.Companion.doStuff1

fun main() {
    MVE("").test()
}

data class MVE(private val parameter: CharSequence) {
    fun test() = parameter.doStuff1()

    private data class Used<caret>(val parameter: CharSequence) {
        companion object {
            fun CharSequence.doStuff1() = Used(this).doStuff2()
        }

        private fun doStuff2() = ""
    }
}