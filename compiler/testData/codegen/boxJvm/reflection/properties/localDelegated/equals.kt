// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: Delegate.kt
import kotlin.reflect.KProperty

object Delegate {
    operator fun getValue(instance: Any?, kProperty: KProperty<*>): KProperty<*> = kProperty

    operator fun setValue(instance: Any?, kProperty: KProperty<*>, value: KProperty<*>) {}
}

// FILE: pkg.kt
import kotlin.reflect.KProperty

fun getInPackage(flag: Boolean): KProperty<*> =
    if (flag) {
        val p by Delegate
        p
    } else {
        val p by Delegate
        p
    }

// FILE: class.kt
import kotlin.reflect.KProperty

object O {
    fun getInClass(flag: Boolean): KProperty<*> =
        if (flag) {
            val p by Delegate
            p
        } else {
            val p by Delegate
            p
        }
}

// FILE: multifile.kt
@file:JvmMultifileClass
@file:JvmName("Multifile")
import kotlin.reflect.KProperty

fun getInMultifileClass(flag: Boolean): KProperty<*> =
    if (flag) {
        val p by Delegate
        p
    } else {
        val p by Delegate
        p
    }

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    assertNotEquals(getInPackage(false), getInPackage(true))
    assertNotEquals(O.getInClass(false), O.getInClass(true))
    assertNotEquals(getInMultifileClass(false), getInMultifileClass(true))
    return "OK"
}
