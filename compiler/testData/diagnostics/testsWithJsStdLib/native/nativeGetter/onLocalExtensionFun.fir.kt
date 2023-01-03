// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

fun foo() {
    @nativeGetter
    fun Int.get(a: String): Int? = 1

    @nativeGetter
    fun Int.get2(a: Number): String? = "OK"

    @nativeGetter
    fun Int.get3(a: Int): String? = "OK"

    @nativeGetter
    fun Int.get(a: Any): Int? = 1

    @nativeGetter
    fun Int.get2(): String? = "OK"

    @nativeGetter
    fun Int.get3(a: Any, b: Int, c: Any?): String? = "OK"

    @nativeGetter
    fun Any.foo(a: Int = 1): Any? = "OK"
}
