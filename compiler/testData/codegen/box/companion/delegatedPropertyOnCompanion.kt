// DONT_TARGET_EXACT_BACKEND: WASM
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): String = "OK"
}

class Delegate2 {
    var value: String = "NOT OK"

    operator fun getValue(t: Any?, p: KProperty<*>): String = value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        this.value = value
    }
}

class A {
    companion object {
        val s: String by Delegate()
        var s2: String by Delegate2()
        var s3: String by Delegate2()
    }

    fun f() = s
    inline fun g() = s

    fun set2() {
        s2 = "OK"
    }
    fun get2() = s2

    inline fun set3() {
        s3 = "OK"
    }
    inline fun get3() = s3
}

fun box(): String {
    val a = A()
    if (a.f() != "OK") return "FAIL0"
    if (a.g() != "OK") return "FAIL0"
    a.set2()
    if (a.get2() != "OK") return "FAIL0"
    a.set3()
    return a.get3()
}