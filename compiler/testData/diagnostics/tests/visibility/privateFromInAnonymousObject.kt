// FIR_IDENTICAL
// FIR_DUMP

class Base {
    private class Private

    fun test() {
        object {
            val x: Private = Private()

            init {
                val y: Private = Private()
            }

            fun foo() {
                val z: Private = Private()
            }
        }
    }
}
