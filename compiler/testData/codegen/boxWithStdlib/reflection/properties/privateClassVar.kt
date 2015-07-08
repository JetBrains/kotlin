import kotlin.reflect.*
import kotlin.reflect.jvm.accessible

class A {
    private var value = 0

    fun ref() = A::class.properties.single() as KMutableProperty1<A, Int>
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
