// !DIAGNOSTICS: -UNUSED_PARAMETER
@jvmName("bar")
fun foo(a: Any) {}

fun Any.foo() {}

@jvmName("barInt")
fun bar(x: List<Int>) {}

@jvmName("barStr")
fun bar(x: List<String>) {}

class C {
    var rwProp: Int
        @jvmName("get_rwProp")
        get() = 0
        @jvmName("set_rwProp")
        set(v) {}

    fun getRwProp(): Int = 123
    fun setRwProp(v: Int) {}
}
