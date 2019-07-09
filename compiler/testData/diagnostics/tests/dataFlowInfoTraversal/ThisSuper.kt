open class Base {
    fun bar(x: Int): Int = x + 1
}

class Derived : Base() {
    fun baz(x: Int): Int = x + 1

    fun foo() {
        val x: Int? = null

        super.bar(<!TYPE_MISMATCH!>x<!>)
        this.baz(<!TYPE_MISMATCH!>x<!>)
        if (x == null) return
        super.bar(<!DEBUG_INFO_SMARTCAST!>x<!>)
        this.baz(<!DEBUG_INFO_SMARTCAST!>x<!>)

        val y: Int? = null
        if (y != null) super.bar(this.baz(<!DEBUG_INFO_SMARTCAST!>y<!>))
        else this.baz(super.bar(<!DEBUG_INFO_CONSTANT, TYPE_MISMATCH!>y<!>))
    }
}
