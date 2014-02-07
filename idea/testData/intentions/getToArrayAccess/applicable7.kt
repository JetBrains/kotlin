// IS_APPLICABLE: true
fun foo() {
	val y = 1;
    val x = X();
    x.get<caret>(y == 1)
}

public class X() {
    fun get(a: Boolean) : Boolean = a;
}