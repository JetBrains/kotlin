// JVM_TARGET: 1.8
// WITH_REFLECT
// KOTLIN_CONFIGURATION_FLAGS: +JVM.INTERFACE_COMPATIBILITY

annotation class Property(val value: String)
annotation class Accessor(val value: String)

interface Z {
    @Property("OK")
    val z: String;
        @Accessor("OK")
        get() = "OK"
}


class Test : Z

fun box() : String {
    val value = (Z::z.annotations.single() as Property).value
    if (value != "OK") return value
    val forName = Class.forName("Z\$DefaultImpls")
    val annotation = forName.getDeclaredMethod("z\$annotations").getAnnotation(Property::class.java)
    return annotation.value
}