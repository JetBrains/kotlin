// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// WITH_REFLECT

var Any.extProp: String
        get() = "extProp"
        set(x: String) {}

fun box(): String {
    val epg = Any::extProp.getter
    val eps = Any::extProp.setter

    if (epg !is Function1<*, *>) return "Failed: epg is Function1<*, *>"
    if (eps !is Function2<*, *, *>) return "Failed: eps is Function2<*, *, *>"

    return "OK"
}
