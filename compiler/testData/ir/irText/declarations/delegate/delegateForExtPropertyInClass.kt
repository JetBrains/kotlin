import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: F.A, p: KProperty<*>): Int = 1
}

class F {
    val A.prop: Int by Delegate()

    class A
}