// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// WITH_REFLECT

var Any.extProp: String
        get() = "extProp"
        set(x: String) {}

fun box(): String {
    val epg = Any::extProp.getter
    val eps = Any::extProp.setter

    assert(epg is Function1<*, *>) { "Failed: epg is Function1<*, *>"}
    assert(eps is Function2<*, *, *>) { "Failed: eps is Function2<*, *, *>"}

    return "OK"
}
