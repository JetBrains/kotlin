// "Replace 'if' expression with elvis expression" "true"
class Test {
    var x: String? = ""

    fun test() {
        val i = if (x != null) {
            <caret>x.length
        } else {
            0
        }
    }
}