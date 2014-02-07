// IS_APPLICABLE: true
fun foo() {
    val x = X()
    x.<caret>get(1,2,3,4)
}

public class X() {
    fun get(a: Int,b: Int,c: Int,d: Int) : Int = a + b + c + d;
}