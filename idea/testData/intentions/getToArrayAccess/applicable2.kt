// IS_APPLICABLE: true
fun foo() {
    val x = X(1)
    x.<caret>get(0).get(1)
}

public class X(init : Int) {
    fun get(a: Int) : X = X(a);
}