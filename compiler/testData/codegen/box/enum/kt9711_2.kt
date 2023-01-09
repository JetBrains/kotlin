// KT-55828
// IGNORE_BACKEND_K2: NATIVE
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