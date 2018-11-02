// "Replace 'if' expression with safe access expression" "true"
// WITH_RUNTIME
class Test {
    var x: String? = ""

    fun test() {
        if (x != null) foo(<caret>x)
    }

    fun foo(s: String) = 1
}