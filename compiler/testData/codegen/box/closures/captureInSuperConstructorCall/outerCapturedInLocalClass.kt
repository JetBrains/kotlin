// IGNORE_BACKEND_FIR: JVM_IR
abstract class Base(val fn: () -> String)

class Outer {
    val ok = "OK"

    fun foo(): String {
        class Local : Base({ ok })

        return Local().fn()
    }
}

fun box() = Outer().foo()