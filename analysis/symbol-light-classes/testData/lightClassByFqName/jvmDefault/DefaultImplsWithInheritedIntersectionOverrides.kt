// B
// JVM_DEFAULT_MODE: disable

interface Generic<T> {
    fun generic(t: T): T = t
}

interface Base {
    fun base() {}

    fun overriddenInLeft() {}

    fun reabstractedInRight() {}
}

interface Left : Base, Generic<String> {
    override fun overriddenInLeft() {}
}

interface Right : Base, Generic<String> {
    abstract override fun reabstractedInRight()
}

interface B : Left, Right
