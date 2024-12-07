// MODULE: m1-common
// FILE: common.kt

import A.Companion.companionExtensionFunction

expect class A() {
    fun memberFunction(x: Int, y: String = "ok")
    companion object {
        fun companionFunction(x: Int, y: String = "ok")
        fun String.companionExtensionFunction(x: Int, y: String = "ok")
    }
}

expect fun topLevelFunction(x: Int, y: String = "ok")

expect fun String.topLevelExtensionFunction(x: Int, y: String = "ok")

fun test() {
    A().memberFunction<!NO_VALUE_FOR_PARAMETER!>()<!>
    A().memberFunction(42)
    A().memberFunction(42, "ok")

    topLevelFunction<!NO_VALUE_FOR_PARAMETER!>()<!>
    topLevelFunction(42)
    topLevelFunction(42, "ok")

    "".topLevelExtensionFunction<!NO_VALUE_FOR_PARAMETER!>()<!>
    "".topLevelExtensionFunction(42)
    "".topLevelExtensionFunction(42, "ok")
}

fun A.test() {
    "".companionExtensionFunction<!NO_VALUE_FOR_PARAMETER!>()<!>
    "".companionExtensionFunction(42)
    "".companionExtensionFunction(42, "ok")
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

import A.Companion.companionExtensionFunction

actual class A {
    actual fun memberFunction(x: Int, y: String) = Unit
    actual companion object {
        actual fun companionFunction(x: Int, y: String) = Unit
        actual fun String.companionExtensionFunction(x: Int, y: String) = Unit
    }
}

actual fun topLevelFunction(x: Int, y: String) = Unit

actual fun String.topLevelExtensionFunction(x: Int, y: String) = Unit

fun testJvm() {
    A().memberFunction<!NO_VALUE_FOR_PARAMETER!>()<!>
    A().memberFunction(42)
    A().memberFunction(42, "ok")

    topLevelFunction<!NO_VALUE_FOR_PARAMETER!>()<!>
    topLevelFunction(42)
    topLevelFunction(42, "ok")

    "".topLevelExtensionFunction<!NO_VALUE_FOR_PARAMETER!>()<!>
    "".topLevelExtensionFunction(42)
    "".topLevelExtensionFunction(42, "ok")
}

fun A.testJvm() {
    "".companionExtensionFunction<!NO_VALUE_FOR_PARAMETER!>()<!>
    "".companionExtensionFunction(42)
    "".companionExtensionFunction(42, "ok")
}
