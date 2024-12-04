// JVM_ABI_K1_K2_DIFF: KT-62714

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