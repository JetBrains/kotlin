//KT-4204 ConstraintSystem erased after resolution completion
package c

public abstract class TestBug1() {

    public fun m3(position: Int) {
        position(m1().second!!)
    }

    public fun m4(position: (Int)->Int) {
        position(m1().second)
    }

    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> fun m1(): Pair<Int, Int>

    private fun position(p: Int) {}

}

//from library
public class Pair<out A, out B> (
    public val first: A,
    public val second: B
)
