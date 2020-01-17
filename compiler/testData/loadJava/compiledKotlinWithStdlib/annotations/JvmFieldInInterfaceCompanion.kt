// IGNORE_BACKEND: JVM_IR

package test

interface I {
    companion object {
        @JvmField
        val x = "x"

        @JvmField
        val y = "y"
    }
}
