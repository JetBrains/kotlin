// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*

var storage = "before"

class A {
    val String.readonly: String
        get() = this

    var String.mutable: String
        get() = storage
        set(value) { storage = value }
}

fun box(): String {
    val props = A::class.memberExtensionProperties
    val readonly = props.single { it.name == "readonly" }
    assert(readonly !is KMutableProperty2<A, *, *>) { "Fail 1: $readonly" }
    val mutable = props.single { it.name == "mutable" }
    assert(mutable is KMutableProperty2<A, *, *>) { "Fail 2: $mutable" }

    val a = A()
    mutable as KMutableProperty2<A, String, String>
    assert(mutable.get(a, "") == "before") { "Fail 3: ${mutable.get(a, "")}" }
    mutable.set(a, "", "OK")
    return mutable.get(a, "")
}
