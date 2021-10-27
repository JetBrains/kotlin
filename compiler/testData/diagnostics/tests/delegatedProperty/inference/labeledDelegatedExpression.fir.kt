import kotlin.reflect.KProperty

class A3 {
    val a: String by l@ MyProperty()

    class MyProperty<T> {}

    operator fun <T> MyProperty<T>.getValue(thisRef: Any?, desc: KProperty<*>): T {
        throw Exception("$thisRef $desc")
    }
}
