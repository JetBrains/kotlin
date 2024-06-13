// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Ann(val value: String)

class C<T, @Ann("OK") U>

fun box(): String =
    C::class.java.typeParameters[1].getAnnotation(Ann::class.java).value
