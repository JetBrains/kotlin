// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM_IR
// WITH_REFLECT

import kotlin.reflect.*

annotation class Anno(val value: Int)

object O {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): Int {
        val anno = prop.annotations.singleOrNull() as? Anno
            ?: error("Fail annotations: ${prop.annotations}")
        return anno.value
    }

    fun checkClass() {
        @Anno(123)
        val p by this
        if (p != 123) error("Fail class: $p")
    }
}

fun checkPackage() {
    @Anno(456)
    val p by O
    if (p != 456) error("Fail package: $p")
}

fun box(): String {
    O.checkClass()
    checkPackage()
    return "OK"
}
