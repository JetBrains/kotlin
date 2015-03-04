import java.lang.annotation.*

Retention(RetentionPolicy.RUNTIME)
annotation class First

class MyClass() {
    public var x: String by Delegate()
        [First] set
}

class Delegate {
    fun get(t: Any?, p: PropertyMetadata): String {
        return "OK"
    }

    fun set(t: Any?, p: PropertyMetadata, i: String) {}
}

fun box(): String {
    val e = javaClass<MyClass>()

    val e1 = e.getDeclaredMethod("setX", javaClass<String>()).getAnnotations()
    if (e1.size() != 1) return "Fail E1 size: ${e1.toList()}"
    if (e1[0].annotationType() != javaClass<First>()) return "Fail: ${e1.toList()}"

    return MyClass().x
}
