// "Replace 'if' expression with safe access expression" "true"
class Test {
    var x: String? = ""

    fun test() {
        if (x != null) <caret>x.length
    }
}