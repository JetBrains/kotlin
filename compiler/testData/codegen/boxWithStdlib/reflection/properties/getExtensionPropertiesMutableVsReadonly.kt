import kotlin.reflect.jvm.kotlin
import kotlin.reflect.KMutableMemberExtensionProperty

var storage = "before"

class A {
    val String.readonly: String
        get() = this

    var String.mutable: String
        get() = storage
        set(value) { storage = value }
}

fun box(): String {
    val props = javaClass<A>().kotlin.getExtensionProperties()
    val readonly = props.single { it.name == "readonly" }
    assert(readonly !is KMutableMemberExtensionProperty<A, *, *>) { "Fail 1: $readonly" }
    val mutable = props.single { it.name == "mutable" }
    assert(mutable is KMutableMemberExtensionProperty<A, *, *>) { "Fail 2: $mutable" }

    val a = A()
    mutable as KMutableMemberExtensionProperty<A, String, String>
    assert(mutable[a, ""] == "before") { "Fail 3: ${mutable.get(a, "")}" }
    mutable[a, ""] = "OK"
    return mutable.get(a, "")
}
