annotation class Anno(val value: KClass<*>)

fun test() {
    class LocalClass {}

    @Anno(LocalClass::class)
    fun loc<caret>al() {}

    local()
}