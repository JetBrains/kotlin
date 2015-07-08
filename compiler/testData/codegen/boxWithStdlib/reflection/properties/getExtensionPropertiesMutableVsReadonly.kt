import kotlin.reflect.jvm.kotlin
import kotlin.reflect.*

var storage = "before"

class A {
    val String.readonly: String
        get() = this

    var String.mutable: String
        get() = storage
        set(value) { storage = value }
}

fun box(): String {
    val props = javaClass<A>().kotlin.extensionProperties
    val readonly = props.single { it.name == "readonly" }
    assert(readonly !is KMutableProperty2<A, *, *>) { "Fail 1: $readonly" }
    val mutable = props.single { it.name == "mutable" }
    assert(mutable is KMutableProperty2<A, *, *>) { "Fail 2: $mutable" }

    val a = A()
    mutable as KMutableProperty2<A, String, String>
    assert(mutable[a, ""] == "before") { "Fail 3: ${mutable.get(a, "")}" }
    mutable[a, ""] = "OK"
    return mutable.get(a, "")
}
