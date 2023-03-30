// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// JVM_TARGET: 1.8
// WITH_REFLECT

annotation class Property(val value: String)
annotation class Accessor(val value: String)

interface Z {
    @Property("OK")
    val z: String
        @Accessor("OK")
        get() = "OK"
}


class Test : Z

fun box() : String {
    val value = Z::z.annotations.filterIsInstance<Property>().single().value
    if (value != "OK") return value
    return (Z::z.getter.annotations.single() as Accessor).value
}
