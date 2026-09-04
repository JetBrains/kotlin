class Final {
    // `open` is redundant in a final class, so it should not be rendered.
    open fun redundantOpen() {}

    protected fun protectedMember() {}
}

open class Open {
    open fun overridable() {}

    fun nonOverridable() {}

    protected open fun protectedOverridable() {}
}

abstract class Abstract {
    abstract fun implicitlyOpen()

    open val overridable: Int
        get() = 0
}
