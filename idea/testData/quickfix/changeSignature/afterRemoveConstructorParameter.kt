// "Remove parameter 'x'" "true"
// DISABLE-ERRORS

open class Base() {
    val y = Base();

    fun f() {
        val base = Base();
    }
}

open class Inherited(x: Int) : Base() {}
