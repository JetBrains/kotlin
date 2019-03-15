// "Replace 'if' expression with elvis expression" "false"
// ACTION: Add non-null asserted (!!) call
// ACTION: Introduce local variable
// DISABLE-ERRORS
class Test {
    var x: String? = ""

    fun test() {
        val i = if (x != null) {
            bar()
            <caret>x.length
        } else {
            0
        }
    }

    fun bar() {}
}