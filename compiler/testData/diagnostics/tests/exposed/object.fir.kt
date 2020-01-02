// From KT-10753
object My : Inter() {
    fun foo(arg: Inter): Inter = arg
    val x: Inter? = null
}

internal open class Inter

// From KT-10799
open class Test {
    protected class Protected

    fun foo(x: Protected) = x

    interface NestedInterface {
        fun create(x: Protected)
    }

    class NestedClass {
        fun create(x: Protected) = x
    }

    object NestedObject {
        fun create(x: Protected) = x
    }

    companion object {
        fun create(x: Protected) = x
    }
}
