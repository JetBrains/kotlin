// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun box(): String {
    fun foo(): Unit {}
    assert(Unit.javaClass.equals(foo().javaClass))
    assert(Unit.javaClass.equals(foo()::class.java))
    return "OK"
}
