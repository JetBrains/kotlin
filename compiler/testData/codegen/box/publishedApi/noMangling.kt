// IGNORE_BACKEND: JS
//WITH_REFLECT
class A {
    @PublishedApi
    internal fun published() = "OK"

    inline fun test() = published()

}

fun box() : String {
    val clazz = A::class.java
    if (clazz.getDeclaredMethod("published") == null) return "fail"
    return "OK"
}