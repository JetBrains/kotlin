// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FULL_JDK

import java.lang.reflect.Modifier
import kotlin.reflect.KProperty

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

class C {
    @Volatile var vol = 1
    @Transient val tra = 1
    @delegate:Transient val del: String by CustomDelegate()

    @Strictfp fun str() {}
    @Synchronized fun sync() {}
}

fun box(): String {
    val c = C::class.java

    if (c.getDeclaredField("vol").getModifiers() and Modifier.VOLATILE == 0) return "Fail: volatile"
    if (c.getDeclaredField("tra").getModifiers() and Modifier.TRANSIENT == 0) return "Fail: transient"
    if (c.getDeclaredField("del\$delegate").getModifiers() and Modifier.TRANSIENT == 0) return "Fail: delegate transient"

    if (c.getDeclaredMethod("str").getModifiers() and Modifier.STRICT == 0) return "Fail: strict"
    if (c.getDeclaredMethod("sync").getModifiers() and Modifier.SYNCHRONIZED == 0) return "Fail: synchronized"

    return "OK"
}
