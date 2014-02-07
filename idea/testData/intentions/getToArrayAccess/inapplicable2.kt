// IS_APPLICABLE: false
fun foo() {
	val x = X();
	x.<caret>get();
}

public class X() {
	fun get() : Int = 1
}