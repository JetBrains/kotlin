// PROBLEM: none
fun foo() {
    val x = X()
    val y = "$<caret>{x.bar()}"
}

public class X() {
    fun bar() : String = "bar"
}