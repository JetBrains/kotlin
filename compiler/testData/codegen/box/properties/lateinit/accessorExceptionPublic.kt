// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

fun <T> eval(fn: () -> T) = fn()

public class A {
    fun getFromClass(): Boolean {
        try {
            val a = str
            return false
        } catch (e: RuntimeException) {
            return true
        }
    }

    fun getFromLambda(): Boolean {
        try {
            val a = eval { str }
            return false
        } catch (e: RuntimeException) {
            return true
        }
    }

    companion object {
        lateinit var str: String

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
    if (!A().getFromLambda()) return "Fail getFromLambda"
    if (!A.getFromCompanion()) return "Fail getFromCompanion"

    return "OK"
}
