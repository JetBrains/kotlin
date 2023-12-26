// JVM_ABI_K1_K2_DIFF: KT-63655
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
