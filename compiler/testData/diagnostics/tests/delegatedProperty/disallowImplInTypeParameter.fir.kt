import kotlin.reflect.KProperty0

val a: Int by A()

class A {
    fun getValue(t: Any?, p: KProperty0<*>): Int = 1
}
