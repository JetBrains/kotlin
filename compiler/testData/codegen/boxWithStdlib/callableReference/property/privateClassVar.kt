import kotlin.reflect.IllegalPropertyAccessException
import kotlin.reflect.KMutableMemberProperty
import kotlin.reflect.jvm.accessible

class A {
    private var value = 0

    fun ref(): KMutableMemberProperty<A, Int> = ::value
}

fun box(): String {
    val a = A()
    val p = a.ref()
    try {
        p.set(a, 1)
        return "Fail: private property is accessible by default"
    } catch(e: IllegalPropertyAccessException) { }

    p.accessible = true

    p.set(a, 2)
    p.get(a)

    p.accessible = false
    try {
        p.set(a, 3)
        return "Fail: setAccessible(false) had no effect"
    } catch(e: IllegalPropertyAccessException) { }

    return "OK"
}
