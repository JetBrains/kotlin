// "Replace 'if' expression with safe access expression" "true"
class Test {
    var x: Any? = null

    fun test() {
        if (x is String) <caret>x.length
    }
}