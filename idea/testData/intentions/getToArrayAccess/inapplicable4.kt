// IS_APPLICABLE: false
fun foo() : Int {
    val x = X()
    var y : Int = 4;
    x.<caret>get(a=1,b={2});
}

public class X() {
    fun get(a: Int, b: () -> Int) : Int = a + b();
}