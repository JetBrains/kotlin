import kotlin.reflect.KProperty

@Retention(AnnotationRetention.RUNTIME)
annotation class First

class MyClass() {
    public var x: String by Delegate()
        @First set
}

class Delegate {
    fun getValue(t: Any?, p: KProperty<*>): String {
        return "OK"
    }

    fun setValue(t: Any?, p: KProperty<*>, i: String) {}
}

fun box(): String {
    val e = javaClass<MyClass>()

    val e1 = e.getDeclaredMethod("setX", javaClass<String>()).getAnnotations()
    if (e1.size() != 1) return "Fail E1 size: ${e1.toList()}"
    if (e1[0].annotationType() != javaClass<First>()) return "Fail: ${e1.toList()}"

    return MyClass().x
}
