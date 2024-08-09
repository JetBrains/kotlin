interface Intf
class A<T0: Intf?> {
    fun func(t: T0): String {
        return "OK"
    }
}

inline fun <reified T1 : Intf?> foo(it: A<T1>, t: T1): String {
    return it.func(t)
}

fun box(): String {
    return foo(A<Nothing?>(), null)
}