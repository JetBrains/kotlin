// OUT_OF_CODE_BLOCK: FALSE
// ERROR: This variable must either have a type annotation or be initialized

class Test {
    class Other {
        fun test() {
            val a<caret>
        }
    }
}