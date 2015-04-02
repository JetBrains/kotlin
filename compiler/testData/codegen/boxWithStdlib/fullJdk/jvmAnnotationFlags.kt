import java.lang.reflect.Modifier

class C {
    volatile var vol = 1
    transient val tra = 1

    strictfp fun str() {}
    synchronized fun sync() {}
}

fun box(): String {
    val c = javaClass<C>()

    if (c.getDeclaredField("vol").getModifiers() and Modifier.VOLATILE == 0) return "Fail: volatile"
    if (c.getDeclaredField("tra").getModifiers() and Modifier.TRANSIENT == 0) return "Fail: transient"

    if (c.getDeclaredMethod("str").getModifiers() and Modifier.STRICT == 0) return "Fail: strict"
    if (c.getDeclaredMethod("sync").getModifiers() and Modifier.SYNCHRONIZED == 0) return "Fail: synchronized"

    return "OK"
}
