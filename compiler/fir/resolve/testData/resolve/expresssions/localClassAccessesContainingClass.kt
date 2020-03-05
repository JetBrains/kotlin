class Outer {
    fun foo() {
        class Local {
            fun bar() {
                val x = y
            }
        }
    }

    val y = ""
}