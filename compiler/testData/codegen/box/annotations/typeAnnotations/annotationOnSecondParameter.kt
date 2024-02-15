// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// IGNORE_BACKEND: ANDROID
// Android is ignored due to KT-66391

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Ann(val value: String)

class C<T, @Ann("OK") U>

fun box(): String =
    C::class.java.typeParameters[1].getAnnotation(Ann::class.java).value
