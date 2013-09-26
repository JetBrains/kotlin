class KClient {
    fun client() {
        fun <caret>foo(s: String): String = ""

        val bar: String = foo("")

        fun bar() {
            fun localFun() = foo("")

            foo("")
        }

        object KClientObj {
            val a = foo("")
        }
    }
}