open class Base {
    fun bar(x: Int): Int = x + 1
}

class Derived : Base() {
    fun baz(x: Int): Int = x + 1

    fun foo() {
        val x: Int? = null

        super.bar(x)
        this.baz(x)
        if (x == null) return
        super.bar(x)
        this.baz(x)

        val y: Int? = null
        if (y != null) super.bar(this.baz(y))
        else this.baz(super.bar(y))
    }
}
