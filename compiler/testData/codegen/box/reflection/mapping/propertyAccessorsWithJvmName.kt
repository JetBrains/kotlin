// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-63984

// WITH_REFLECT

import kotlin.reflect.jvm.*

var state: String = "value"
    @JvmName("getter")
    get
    @JvmName("setter")
    set

fun box(): String {
    val p = ::state

    if (p.name != "state") return "Fail name: ${p.name}"
    if (p.get() != "value") return "Fail get: ${p.get()}"
    p.set("OK")

    val getterName = p.javaGetter!!.getName()
    if (getterName != "getter") return "Fail getter name: $getterName"

    val setterName = p.javaSetter!!.getName()
    if (setterName != "setter") return "Fail setter name: $setterName"

    return p.get()
}
