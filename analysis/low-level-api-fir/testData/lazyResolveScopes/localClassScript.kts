package lowlevel

fun foo() {
    class MyClas<caret>s {
        fun function(i: Int) = i
        val property = function(1)
    }
}
