// IS_APPLICABLE: false
fun foo() {
    <caret>val x = X(1)
    x.get(0).get(1)
}

public class X(init : Int) {
    fun get(a: Int) : X = X(a);
}