// IGNORE_BACKEND_FIR: JVM_IR

class C {

    val s = "OK"

    private val localObject = object {
        fun getS(): String {
            return s
        }
    }

    fun ok(): String =
        33.let { localObject.getS() }
}

fun box() = C().ok()
