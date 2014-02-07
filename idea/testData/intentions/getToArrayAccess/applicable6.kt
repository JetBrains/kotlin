// IS_APPLICABLE: true
fun foo() : Int {
    val x = X()
    var y : Int = 4;
    return x.g<caret>et(1) {
        if(10 > 9) {
            4
        } else {
            3
        }
    }
}

public class X() {
    fun get(a: Int, b: () -> Int) : Int = a + b();
}