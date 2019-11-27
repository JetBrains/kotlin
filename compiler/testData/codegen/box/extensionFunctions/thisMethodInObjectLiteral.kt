// IGNORE_BACKEND_FIR: JVM_IR
class Test {
    private fun <T : Any> T.self() = object{
        fun calc() : T {
            return this@self
        }
    }

    fun box() : Int  {
        return 1.self().calc() + 1
    }
}

fun box() : String {
    return if (Test().box() == 2) "OK" else "fail"
}