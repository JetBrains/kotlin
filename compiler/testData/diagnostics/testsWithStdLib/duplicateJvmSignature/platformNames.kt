// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.platform.*

@platformName("bar")
fun foo(a: Any) {}

fun Any.foo() {}

@platformName("barInt")
fun bar(x: List<Int>) {}

@platformName("barStr")
fun bar(x: List<String>) {}

class C {
    var rwProp: Int
        @platformName("get_rwProp")
        get() = 0
        @platformName("set_rwProp")
        set(v) {}

    fun getRwProp(): Int = 123
    fun setRwProp(v: Int) {}
}
