public class C {
    public val foo: Int = 0

    public fun bar() {}

}

open class D {
    protected open fun willRemainProtected() {
    }

    protected open fun willBecomePublic() {
    }
}

class E : D() {
    protected override fun willRemainProtected() {
    }

    public override fun willBecomePublic() {
    }
}

enum class F private constructor(val x: Int) {
    FIRST(42)
}

sealed class G constructor(val y: Int) {
    private constructor(): this(42)

    object H : G()
}

interface I {
    fun bar()
}

fun f() {
    val i = object : I {
        internal var foo = 0
        override fun bar() {}
    }
    i.foo = 1

    class LocalClass {
        internal var foo = 0
    }
    LocalClass().foo = 1
}