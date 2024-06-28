// ISSUE: KT-51753
// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// WITH_REFLECT

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common/AtomicBoolean.kt

import kotlin.reflect.KProperty

expect class AtomicRef<T> {
    var value: T

    inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T

    inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
}

expect fun <T> atomic(initial: T): AtomicRef<T>

// FILE: common/test.kt

private val _topLevelRef = atomic("A")
var topLevelDelegatedPropertyRef: String by _topLevelRef

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM
// FILE: jvm/AtomicBoolean.kt

import kotlin.reflect.KProperty

actual class AtomicRef<T> internal constructor(v: T) {

    actual inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    actual inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    actual var value: T = v
}

actual fun <T> atomic(initial: T): AtomicRef<T> = AtomicRef(initial)

// FILE: jvm/box.kt

fun box(): String {
    val s = topLevelDelegatedPropertyRef
    return if (s == "A") "OK" else "FAIL($s)"
}
