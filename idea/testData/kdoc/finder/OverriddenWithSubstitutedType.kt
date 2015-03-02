open class Foo<T>() {
    /**
     * Doc for method xyzzy
     */
    open fun xyzzy(): Int = 0
}

open class Bar(): Foo<String>() {
    override fun xyzzy(): Int = 1
}
