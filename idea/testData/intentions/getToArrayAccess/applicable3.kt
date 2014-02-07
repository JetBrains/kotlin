// IS_APPLICABLE: true
fun foo() {
    val x = X(1)
    x.get(0).<caret>get(1)
}

public class X(init : Int) {
    fun get(a: Int) : X = X(a);
}