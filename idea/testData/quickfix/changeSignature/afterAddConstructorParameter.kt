// "Add parameter to constructor 'Base'" "true"
// DISABLE-ERRORS

open class Base(var x: Int,
                d: Double) {
    val y = Base(1, 2.5);

    fun f() {
        val base = Base(1, 2.5);
    }
}

open class Inherited(x: Int) : Base(1, 2.5) {}
