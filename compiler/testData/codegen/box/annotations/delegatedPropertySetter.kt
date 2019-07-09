// TARGET_BACKEND: JVM

// WITH_RUNTIME

import kotlin.reflect.KProperty

@Retention(AnnotationRetention.RUNTIME)
annotation class First

class MyClass() {
    public var x: String by Delegate()
        @First set
}

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): String {
        return "OK"
    }

    operator fun setValue(t: Any?, p: KProperty<*>, i: String) {}
}

fun box(): String {
    val e = MyClass::class.java

    val e1 = e.getDeclaredMethod("setX", String::class.java).getAnnotations()
    if (e1.size != 1) return "Fail E1 size: ${e1.toList()}"
    if (e1[0].annotationClass.java != First::class.java) return "Fail: ${e1.toList()}"

    return MyClass().x
}
