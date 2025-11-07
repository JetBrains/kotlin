// TARGET_BACKEND: JVM

// WITH_STDLIB
// FULL_JDK

import java.lang.reflect.Modifier
import kotlin.reflect.KProperty

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

class C {
    @Volatile var vol = 1
    @kotlin.concurrent.Volatile var vol2 = 1
    @Transient val tra = 1
    @delegate:Transient val del: String by CustomDelegate()

    @Strictfp fun str() {}
    @Synchronized fun sync() {}

    @JvmSynthetic val synth = "ABC"

    var synth2 = 5
        @JvmSynthetic public get
        @JvmSynthetic public set

    @field:JvmSynthetic
    val synth3 = 0

    @get:JvmSynthetic @set:JvmSynthetic
    var synth4 = 0

    @JvmSynthetic
    fun synth5() {}
}

fun box(): String {
    val c = C::class.java

    if (c.getDeclaredField("vol").getModifiers() and Modifier.VOLATILE == 0) return "Fail: volatile"
    if (c.getDeclaredField("vol2").getModifiers() and Modifier.VOLATILE == 0) return "Fail: volatile from kotlin.concurrent"
    if (c.getDeclaredField("tra").getModifiers() and Modifier.TRANSIENT == 0) return "Fail: transient"
    if (c.getDeclaredField("del\$delegate").getModifiers() and Modifier.TRANSIENT == 0) return "Fail: delegate transient"

    if (c.getDeclaredMethod("str").getModifiers() and Modifier.STRICT == 0) return "Fail: strict"
    if (c.getDeclaredMethod("sync").getModifiers() and Modifier.SYNCHRONIZED == 0) return "Fail: synchronized"

    if (!c.getDeclaredField("synth").isSynthetic()) return "Fail: synthetic"
    if (c.getDeclaredMethod("getSynth").isSynthetic()) return "Fail: get synthetic"

    if (c.getDeclaredField("synth2").isSynthetic()) return "Fail: synthetic 2"
    if (!c.getDeclaredMethod("getSynth2").isSynthetic()) return "Fail: get synthetic 2"
    if (!c.getDeclaredMethod("setSynth2", Int::class.java).isSynthetic()) return "Fail: set synthetic 2"

    if (!c.getDeclaredField("synth3").isSynthetic()) return "Fail: synthetic 3"
    if (c.getDeclaredMethod("getSynth3").isSynthetic()) return "Fail: get synthetic 3"

    if (c.getDeclaredField("synth4").isSynthetic()) return "Fail: synthetic 4"
    if (!c.getDeclaredMethod("getSynth4").isSynthetic()) return "Fail: get synthetic 4"
    if (!c.getDeclaredMethod("setSynth4", Int::class.java).isSynthetic()) return "Fail: set synthetic 4"

    if (!c.getDeclaredMethod("synth5").isSynthetic()) return "Fail: synthetic 5"

    return "OK"
}
