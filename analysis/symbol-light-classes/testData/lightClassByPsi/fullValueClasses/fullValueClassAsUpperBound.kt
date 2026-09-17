// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

value class Some(val value: String, val count: Int)

abstract value class AbstractSome {
    abstract val value: String
}

class RegularClass {
    fun <T : Some> classFunInParameter(t: T) {}
    fun <T : Some> classFunInReturn(): T = TODO()
    fun <T : Some> T.classFunInExtension() {}

    var <T : Some> T.classPropInExtension: Int
        get() = 1
        set(value) {}

    @JvmName("specialName")
    fun <T : Some> classFunWithJvmName(t: T) {}

    fun <T : AbstractSome> abstractInParameter(t: T) {}
    fun <T : AbstractSome> abstractInReturn(): T = TODO()
}

interface RegularInterface {
    fun <T : Some> interfaceFunInParameter(t: T)
    fun <T : Some> interfaceFunInReturn(): T
}

fun <T : Some> topLevelFunInParameter(t: T) {}
fun <T : Some> topLevelFunInReturn(): T = TODO()

// DECLARATIONS_NO_LIGHT_ELEMENTS: AbstractSome.class[equals;hashCode;toString]
