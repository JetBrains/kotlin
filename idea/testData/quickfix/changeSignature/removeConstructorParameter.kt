// "Remove parameter 'x'" "true"
// DISABLE-ERRORS

open class Base(var x: Int) {
    val y = Base(1);

    fun f() {
        val base = Base(1, 2);
    }
}

open class Inherited(x: Int) : Base(<caret>) {}
