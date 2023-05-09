interface Base {
    val x: Int
}

open class Impl(override val x: Int) : Base {
    init {
        if (this.x != 0) foo()
    }
}

fun foo() {}
