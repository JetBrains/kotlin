// "Change parameter 'a' type of primary constructor of class 'B' to 'String'" "true"
class B(val a: Int)
fun foo() {
    B(if (true) ""<caret> else "")
}
