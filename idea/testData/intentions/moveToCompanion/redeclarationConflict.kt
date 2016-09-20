// SHOULD_FAIL_WITH: Companion object already contains function f1(Int)
class Test9{
    fun <caret>f1(n: Int){}
    companion object{
        fun f1(n: Int){}
    }
}