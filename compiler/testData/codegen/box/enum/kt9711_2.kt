// IGNORE_BACKEND_FIR: JVM_IR
enum class IssueState {

    FIXED {
        override fun ToString() = D().k

        fun s()  = "OK"

        inner class D {
            val k = s()
        }
    };

    open fun ToString() : String = "fail"
}

fun box(): String {
    return IssueState.FIXED.ToString()
}