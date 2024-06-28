import kotlin.reflect.KProperty

fun foo(vararg x: KProperty<*>) {}

var prop: String = ""

fun test() {
    foo(::prop)
}

fun box(): String {
    test()
    return "OK"
}