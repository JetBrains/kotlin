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
