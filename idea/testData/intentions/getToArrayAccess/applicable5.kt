// IS_APPLICABLE: true
fun foo() {
    val x = X();
    x.<caret>get("XyZ",1,3,"k");
}

public class X() {
    fun get(a: String, b: Int, c: Int, d: String) : String = a + b + c + d;
}