// IGNORE_BACKEND_FIR: JVM_IR
public class A {
    fun getFromClass(): Boolean {
        try {
            val a = str
            return false
        } catch (e: RuntimeException) {
            return true
        }
    }

    fun getFromCompanion() = Companion.getFromCompanion()

    private companion object {
        private lateinit var str: String

        fun getFromCompanion(): Boolean {
            try {
                val a = str
                return false
            } catch (e: RuntimeException) {
                return true
            }
        }
    }
}

fun box(): String {
    if (!A().getFromClass()) return "Fail getFromClass"
    if (!A().getFromCompanion()) return "Fail getFromCompanion"

    return "OK"
}
