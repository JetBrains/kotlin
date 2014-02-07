// IS_APPLICABLE: true
fun foo() {
	val x = X();
    x.<caret>get("XyZ");
}

public class X() {
    fun get(a: String) : String = a;
}