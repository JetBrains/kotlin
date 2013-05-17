// "Change parameter 'a' type of class 'B' primary constructor to 'String'" "true"
class B(val a: String)
fun foo() {
    B(if (true) ""<caret> else "")
}
