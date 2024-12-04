// RUN_PIPELINE_TILL: BACKEND
class My {
    companion object {
        fun My.foo() {}
    }

    fun test() {
        foo()
    }
}
