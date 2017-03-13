// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

enum class IssueState {

    FIXED {
        override fun ToString() = D().k

        fun s()  = "OK"

        class D {
            val k = s()
        }
    };

    open fun ToString() : String = "fail"
}

fun box(): String {
    return IssueState.FIXED.ToString()
}