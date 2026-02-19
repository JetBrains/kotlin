// TARGET_BACKEND: JVM_IR
// EMIT_JVM_TYPE_ANNOTATIONS
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK

@Target(AnnotationTarget.TYPE)
annotation class Ann(val value: String)

interface I1
interface I2

open class A
class C : @Ann("OK") A(), I1, @Ann("I2") I2

fun box(): String {
    val i2 = C::class.java.annotatedInterfaces[1].annotations.single { it is Ann }
    if ((i2 as Ann).value != "I2") return "Fail"

    val a = C::class.java.annotatedSuperclass.annotations.single { it is Ann }
    return (a as Ann).value
}
