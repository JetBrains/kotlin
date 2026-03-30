// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB

fun box(): String {
    fun foo(): Unit {}
    assert(Unit.javaClass.equals(foo().javaClass))
    assert(Unit.javaClass.equals(foo()::class.java))
    return "OK"
}
