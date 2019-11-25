// IGNORE_BACKEND_FIR: JVM_IR
abstract class Base(val fn: () -> String)

open class Outer {
    val outerO = "O"

    fun test(): Base {
        val localK = "K"
        class Local : Base({ outerO + localK })

        return Local()
    }
}

fun box() = Outer().test().fn()