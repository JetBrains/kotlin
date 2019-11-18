// !LANGUAGE: +UseGetterNameForPropertyAnnotationsMethodOnJvm
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KProperty2

class C {
    @Deprecated("int")
    @get:JvmName("mapIntIntHex")
    val Map<Int, Int>.hex: String get() = "O"

    @Deprecated("byte")
    @get:JvmName("mapIntByteHex")
    val Map<Int, Byte>.hex: String get() = "K"
}

fun box(): String {
    val a1 = C::class.members.single {
        it is KProperty2<*, *, *> && it.parameters[1].type.arguments[1].type!!.classifier == Int::class
    }.annotations
    if ((a1.single() as Deprecated).message != "int")
        return "Fail annotations on Map<Int, Int>::hex: $a1"

    val a2 = C::class.members.single {
        it is KProperty2<*, *, *> && it.parameters[1].type.arguments[1].type!!.classifier == Byte::class
    }.annotations
    if ((a2.single() as Deprecated).message != "byte")
        return "Fail annotations on Map<Int, Byte>::hex: $a2"

    return with(C()) { mapOf(0 to 0).hex + mapOf(0 to 0.toByte()).hex }
}
