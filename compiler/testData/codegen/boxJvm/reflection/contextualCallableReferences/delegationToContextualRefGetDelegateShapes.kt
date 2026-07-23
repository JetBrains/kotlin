// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.reflect.KProperty0
import kotlin.reflect.full.declaredMemberExtensionProperties
import kotlin.reflect.full.getExtensionDelegate
import kotlin.reflect.jvm.isAccessible

context(c1: Int, c2: String)
val contextualizedProp: String
    get() = c2 + c1

context(c: String)
val Int.decorated: String
    get() = c + this

fun box(): String = context(1, "K") {
    class B {
        // the delegate captures the bound extension receiver 5 and the bound context argument "K"
        val y by 5::decorated

        // member extension property delegated to a contextual reference
        val String.w: String by ::contextualizedProp
    }

    val yProp = B::y
    yProp.isAccessible = true
    val yDelegate = yProp.getDelegate(B())
    if (yDelegate !is KProperty0<*>) return@context "FAIL 1: $yDelegate"
    if (yDelegate.get() != "K5") return@context "FAIL 2: ${yDelegate.get()}"
    // the delegate must carry both the bound receiver and the bound context arguments
    if (yDelegate != 5::decorated) return@context "FAIL 3"

    val wProp = B::class.declaredMemberExtensionProperties.single()
    wProp.isAccessible = true
    val wDelegate = wProp.getExtensionDelegate(B())
    if (wDelegate !is KProperty0<*>) return@context "FAIL 4: $wDelegate"
    if (wDelegate.get() != "K1") return@context "FAIL 5: ${wDelegate.get()}"
    if (wDelegate != ::contextualizedProp) return@context "FAIL 6"

    "OK"
}
